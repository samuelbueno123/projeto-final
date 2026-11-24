package com.cefet_rj.projeto_final.shared.observability;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.WriteListener;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;
import java.util.regex.Pattern;

/**
 * Instrumentação leve e real de tráfego HTTP, códigos de status, vazão de bytes e latência (p50/p95/p99).
 * Não armazena query strings, cabeçalhos sensíveis, corpos nem dados pessoais.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class HttpTelemetryFilter extends OncePerRequestFilter {

    private static final int LATENCY_RING_SIZE = 1000;
    private static final int ENDPOINT_RING_SIZE = 64;
    private static final int MAX_ENDPOINTS_TRACKED = 250;
    private static final int MAX_SLOW_OPS = 50;
    private static final long SLOW_THRESHOLD_MS = 300L;

    private static final Pattern UUID_SEGMENT = Pattern.compile("/[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}");
    private static final Pattern NUMERIC_SEGMENT = Pattern.compile("/\\d+(?=/|$)");

    private final LongAdder totalRequests = new LongAdder();
    private final AtomicInteger inFlightRequests = new AtomicInteger(0);
    private final LongAdder status2xx = new LongAdder();
    private final LongAdder status3xx = new LongAdder();
    private final LongAdder status4xx = new LongAdder();
    private final LongAdder status5xx = new LongAdder();
    private final LongAdder bytesIn = new LongAdder();
    private final LongAdder bytesOut = new LongAdder();

    private final LongAdder bucketUnder25 = new LongAdder();
    private final LongAdder bucket25To50 = new LongAdder();
    private final LongAdder bucket50To100 = new LongAdder();
    private final LongAdder bucket100To250 = new LongAdder();
    private final LongAdder bucket250To500 = new LongAdder();
    private final LongAdder bucket500To1000 = new LongAdder();
    private final LongAdder bucketOver1000 = new LongAdder();

    private final long[] latencyRing = new long[LATENCY_RING_SIZE];
    private int latencyRingIndex = 0;
    private int latencyRingCount = 0;
    private final Object latencyLock = new Object();
    private final AtomicLong maxLatencyMs = new AtomicLong(0);

    private final ConcurrentLinkedDeque<Long> recentRequestTimestamps = new ConcurrentLinkedDeque<>();
    private final ConcurrentLinkedDeque<Long> recent5xxTimestamps = new ConcurrentLinkedDeque<>();
    private final ConcurrentMap<String, EndpointStats> endpointStats = new ConcurrentHashMap<>();
    private final Deque<SlowOperation> slowOperations = new ArrayDeque<>(MAX_SLOW_OPS);
    private final Object slowOpsLock = new Object();

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String rawUri = request.getRequestURI();
        boolean isSseStream = isStreamingPath(rawUri);

        totalRequests.increment();
        long nowMs = System.currentTimeMillis();
        recentRequestTimestamps.addLast(nowMs);
        trimOldTimestamps(recentRequestTimestamps, nowMs - 60_000L);

        long contentLength = request.getContentLengthLong();
        if (contentLength > 0) {
            bytesIn.add(contentLength);
        }

        if (isSseStream) {
            filterChain.doFilter(request, response);
            return;
        }

        inFlightRequests.incrementAndGet();
        long startNs = System.nanoTime();
        CountingResponseWrapper wrappedResponse = new CountingResponseWrapper(response);

        try {
            filterChain.doFilter(request, wrappedResponse);
        } finally {
            inFlightRequests.decrementAndGet();
            long durationMs = Math.max(0L, (System.nanoTime() - startNs) / 1_000_000L);
            int status = wrappedResponse.getStatus();
            long outCount = wrappedResponse.getBytesWritten();
            if (outCount <= 0) {
                String clHeader = wrappedResponse.getHeader("Content-Length");
                if (clHeader != null) {
                    try {
                        outCount = Math.max(0L, Long.parseLong(clHeader.trim()));
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
            if (outCount > 0) {
                bytesOut.add(outCount);
            }

            recordStatus(status, System.currentTimeMillis());
            recordLatency(durationMs);

            String normalizedPath = normalizePath(rawUri);
            String endpointKey = request.getMethod() + " " + normalizedPath;
            recordEndpoint(endpointKey, status, durationMs);

            if (durationMs >= SLOW_THRESHOLD_MS) {
                recordSlowOperation(request.getMethod(), normalizedPath, status, durationMs);
            }
        }
    }

    private boolean isStreamingPath(String uri) {
        if (uri == null) return false;
        return uri.endsWith("/admin/telemetry/stream")
                || uri.endsWith("/realtime/events")
                || uri.endsWith("/realtime/subscribe");
    }

    private String normalizePath(String uri) {
        if (uri == null || uri.isBlank()) return "/";
        String normalized = UUID_SEGMENT.matcher(uri).replaceAll("/{uuid}");
        normalized = NUMERIC_SEGMENT.matcher(normalized).replaceAll("/{id}");
        return normalized;
    }

    private void recordStatus(int status, long nowMs) {
        if (status >= 200 && status < 300) {
            status2xx.increment();
        } else if (status >= 300 && status < 400) {
            status3xx.increment();
        } else if (status >= 400 && status < 500) {
            status4xx.increment();
        } else if (status >= 500) {
            status5xx.increment();
            recent5xxTimestamps.addLast(nowMs);
            trimOldTimestamps(recent5xxTimestamps, nowMs - 60_000L);
        }
    }

    private void recordLatency(long durationMs) {
        synchronized (latencyLock) {
            latencyRing[latencyRingIndex] = durationMs;
            latencyRingIndex = (latencyRingIndex + 1) % LATENCY_RING_SIZE;
            if (latencyRingCount < LATENCY_RING_SIZE) {
                latencyRingCount++;
            }
        }
        maxLatencyMs.accumulateAndGet(durationMs, Math::max);

        if (durationMs < 25) bucketUnder25.increment();
        else if (durationMs < 50) bucket25To50.increment();
        else if (durationMs < 100) bucket50To100.increment();
        else if (durationMs < 250) bucket100To250.increment();
        else if (durationMs < 500) bucket250To500.increment();
        else if (durationMs < 1000) bucket500To1000.increment();
        else bucketOver1000.increment();
    }

    private void recordEndpoint(String key, int status, long durationMs) {
        EndpointStats stats = endpointStats.get(key);
        if (stats == null) {
            if (endpointStats.size() >= MAX_ENDPOINTS_TRACKED) {
                return;
            }
            stats = endpointStats.computeIfAbsent(key, k -> new EndpointStats());
        }
        stats.record(status >= 400, durationMs);
    }

    private void recordSlowOperation(String method, String endpoint, int status, long durationMs) {
        SlowOperation op = new SlowOperation(
                Instant.now().toString(),
                method,
                endpoint,
                status,
                durationMs
        );
        synchronized (slowOpsLock) {
            if (slowOperations.size() >= MAX_SLOW_OPS) {
                slowOperations.removeFirst();
            }
            slowOperations.addLast(op);
        }
    }

    private void trimOldTimestamps(ConcurrentLinkedDeque<Long> deque, long cutoffMs) {
        while (true) {
            Long head = deque.peekFirst();
            if (head == null || head >= cutoffMs) {
                break;
            }
            deque.pollFirst();
        }
    }

    public HttpSnapshot getSnapshot() {
        long nowMs = System.currentTimeMillis();
        trimOldTimestamps(recentRequestTimestamps, nowMs - 60_000L);
        trimOldTimestamps(recent5xxTimestamps, nowMs - 60_000L);

        long[] copy;
        synchronized (latencyLock) {
            copy = Arrays.copyOf(latencyRing, latencyRingCount);
        }
        Arrays.sort(copy);

        double avg = 0.0;
        if (copy.length > 0) {
            long sum = 0;
            for (long v : copy) sum += v;
            avg = round1((double) sum / copy.length);
        }
        long p50 = percentile(copy, 0.50);
        long p95 = percentile(copy, 0.95);
        long p99 = percentile(copy, 0.99);

        long total = totalRequests.sum();
        long s2xx = status2xx.sum();
        long s3xx = status3xx.sum();
        long s4xx = status4xx.sum();
        long s5xx = status5xx.sum();
        long completed = s2xx + s3xx + s4xx + s5xx;
        double successRate = completed > 0
                ? round1(((double) (s2xx + s3xx) / completed) * 100.0)
                : 100.0;

        List<Map<String, Object>> histogram = List.of(
                Map.of("label", "<25ms", "count", bucketUnder25.sum()),
                Map.of("label", "25-50ms", "count", bucket25To50.sum()),
                Map.of("label", "50-100ms", "count", bucket50To100.sum()),
                Map.of("label", "100-250ms", "count", bucket100To250.sum()),
                Map.of("label", "250-500ms", "count", bucket250To500.sum()),
                Map.of("label", "500ms-1s", "count", bucket500To1000.sum()),
                Map.of("label", ">1s", "count", bucketOver1000.sum())
        );

        return new HttpSnapshot(
                total,
                recentRequestTimestamps.size(),
                inFlightRequests.get(),
                s2xx,
                s3xx,
                s4xx,
                s5xx,
                recent5xxTimestamps.size(),
                successRate,
                avg,
                p50,
                p95,
                p99,
                maxLatencyMs.get(),
                bytesIn.sum(),
                bytesOut.sum(),
                histogram
        );
    }

    public List<Map<String, Object>> getTopEndpoints(int limit) {
        return endpointStats.entrySet().stream()
                .map(e -> e.getValue().toMap(e.getKey()))
                .sorted(Comparator.<Map<String, Object>, Long>comparing(m -> ((Number) m.get("count")).longValue()).reversed())
                .limit(limit)
                .toList();
    }

    public List<Map<String, Object>> getSlowestEndpoints(int limit) {
        return endpointStats.entrySet().stream()
                .map(e -> e.getValue().toMap(e.getKey()))
                .sorted(Comparator.<Map<String, Object>, Long>comparing(m -> ((Number) m.get("p95Ms")).longValue()).reversed())
                .limit(limit)
                .toList();
    }

    public List<Map<String, Object>> getErrorEndpoints(int limit) {
        return endpointStats.entrySet().stream()
                .map(e -> e.getValue().toMap(e.getKey()))
                .filter(m -> ((Number) m.get("errorCount")).longValue() > 0)
                .sorted(Comparator.<Map<String, Object>, Long>comparing(m -> ((Number) m.get("errorCount")).longValue()).reversed())
                .limit(limit)
                .toList();
    }

    public List<Map<String, Object>> getSlowOperations(int limit) {
        synchronized (slowOpsLock) {
            List<Map<String, Object>> list = new ArrayList<>();
            Iterator<SlowOperation> it = slowOperations.descendingIterator();
            while (it.hasNext() && list.size() < limit) {
                SlowOperation op = it.next();
                list.add(Map.of(
                        "timestamp", op.timestamp(),
                        "method", op.method(),
                        "endpoint", op.endpoint(),
                        "status", op.status(),
                        "durationMs", op.durationMs()
                ));
            }
            return list;
        }
    }

    private static long percentile(long[] sorted, double p) {
        if (sorted.length == 0) return 0L;
        int idx = (int) Math.ceil(p * sorted.length) - 1;
        idx = Math.max(0, Math.min(sorted.length - 1, idx));
        return sorted[idx];
    }

    private static double round1(double v) {
        return Math.round(v * 10.0) / 10.0;
    }

    public record HttpSnapshot(
            long totalRequests,
            long requestsPerMinute,
            int inFlightRequests,
            long status2xx,
            long status3xx,
            long status4xx,
            long status5xx,
            long recent5xxCount,
            double successRatePercent,
            double avgLatencyMs,
            long p50LatencyMs,
            long p95LatencyMs,
            long p99LatencyMs,
            long maxLatencyMs,
            long bytesIn,
            long bytesOut,
            List<Map<String, Object>> latencyHistogram
    ) {}

    private record SlowOperation(String timestamp, String method, String endpoint, int status, long durationMs) {}

    private static final class EndpointStats {
        private final LongAdder count = new LongAdder();
        private final LongAdder errorCount = new LongAdder();
        private final LongAdder totalDurationMs = new LongAdder();
        private final AtomicLong maxDurationMs = new AtomicLong(0);
        private final long[] ring = new long[ENDPOINT_RING_SIZE];
        private int ringIdx = 0;
        private int ringLen = 0;

        void record(boolean isError, long durationMs) {
            count.increment();
            if (isError) errorCount.increment();
            totalDurationMs.add(durationMs);
            maxDurationMs.accumulateAndGet(durationMs, Math::max);
            synchronized (ring) {
                ring[ringIdx] = durationMs;
                ringIdx = (ringIdx + 1) % ENDPOINT_RING_SIZE;
                if (ringLen < ENDPOINT_RING_SIZE) ringLen++;
            }
        }

        Map<String, Object> toMap(String endpoint) {
            long c = count.sum();
            long err = errorCount.sum();
            long totalMs = totalDurationMs.sum();
            double avg = c > 0 ? round1((double) totalMs / c) : 0.0;
            long[] copy;
            synchronized (ring) {
                copy = Arrays.copyOf(ring, ringLen);
            }
            Arrays.sort(copy);
            long p95 = percentile(copy, 0.95);
            double errorRate = c > 0 ? round1(((double) err / c) * 100.0) : 0.0;

            Map<String, Object> map = new LinkedHashMap<>();
            map.put("endpoint", endpoint);
            map.put("count", c);
            map.put("errorCount", err);
            map.put("errorRatePercent", errorRate);
            map.put("avgMs", avg);
            map.put("p95Ms", p95);
            map.put("maxMs", maxDurationMs.get());
            return map;
        }
    }

    private static final class CountingResponseWrapper extends HttpServletResponseWrapper {
        private final AtomicLong counter = new AtomicLong(0);
        private CountingServletOutputStream countingStream;
        private PrintWriter writer;

        CountingResponseWrapper(HttpServletResponse response) {
            super(response);
        }

        long getBytesWritten() {
            if (writer != null) {
                writer.flush();
            }
            return counter.get();
        }

        @Override
        public ServletOutputStream getOutputStream() throws IOException {
            if (countingStream == null) {
                countingStream = new CountingServletOutputStream(super.getOutputStream(), counter);
            }
            return countingStream;
        }

        @Override
        public PrintWriter getWriter() throws IOException {
            if (writer == null) {
                String enc = getCharacterEncoding();
                writer = new PrintWriter(new OutputStreamWriter(
                        getOutputStream(),
                        enc != null ? enc : StandardCharsets.UTF_8.name()
                ));
            }
            return writer;
        }

        @Override
        public void flushBuffer() throws IOException {
            if (writer != null) writer.flush();
            super.flushBuffer();
        }
    }

    private static final class CountingServletOutputStream extends ServletOutputStream {
        private final ServletOutputStream delegate;
        private final AtomicLong counter;

        CountingServletOutputStream(ServletOutputStream delegate, AtomicLong counter) {
            this.delegate = delegate;
            this.counter = counter;
        }

        @Override
        public void write(int b) throws IOException {
            delegate.write(b);
            counter.incrementAndGet();
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            delegate.write(b, off, len);
            counter.addAndGet(len);
        }

        @Override
        public boolean isReady() {
            return delegate.isReady();
        }

        @Override
        public void setWriteListener(WriteListener writeListener) {
            delegate.setWriteListener(writeListener);
        }

        @Override
        public void flush() throws IOException {
            delegate.flush();
        }

        @Override
        public void close() throws IOException {
            delegate.close();
        }
    }
}
