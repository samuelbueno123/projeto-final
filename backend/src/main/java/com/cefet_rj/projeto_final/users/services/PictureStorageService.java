package com.cefet_rj.projeto_final.users.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Base64;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class PictureStorageService {

    public static final String PICTURES_DB_PREFIX = "media/pictures/";
    private static final long MAX_PICTURE_BYTES = 5L * 1024L * 1024L;
    private static final Pattern DATA_IMAGE_PATTERN = Pattern.compile(
            "^data:image/(png|jpeg|jpg|webp);base64,([A-Za-z0-9+/=\\r\\n]+)$"
    );

    private final Path storageDirectory;

    public PictureStorageService(
            @Value("${app.media.pictures-dir:media/pictures}") String configuredDir
    ) {
        this.storageDirectory = Paths.get(configuredDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.storageDirectory);
        } catch (IOException e) {
            throw new IllegalStateException("Não foi possível criar o diretório de fotos: " + this.storageDirectory, e);
        }
    }

    public Path getStorageDirectory() {
        return storageDirectory;
    }

    /**
     * Recebe um valor enviado no update de perfil:
     * - null ou vazio: remove foto anterior e retorna null
     * - caminho existente em media/pictures/<arquivo>: mantém o caminho relativo no banco
     * - data URL base64: decodifica os bytes, salva o arquivo em media/pictures/<arquivo> e retorna apenas o caminho "media/pictures/<arquivo>"
     */
    public String resolveOrStoreProfileImage(Long userId, String inputImage, String existingPath) {
        if (inputImage == null || inputImage.isBlank()) {
            deleteFileIfPresent(existingPath);
            return null;
        }

        String trimmed = inputImage.trim();

        if (trimmed.startsWith(PICTURES_DB_PREFIX) || trimmed.startsWith("/" + PICTURES_DB_PREFIX)) {
            String normalized = trimmed.startsWith("/") ? trimmed.substring(1) : trimmed;
            String fileName = sanitizeFileName(normalized.substring(PICTURES_DB_PREFIX.length()));
            return PICTURES_DB_PREFIX + fileName;
        }

        Matcher matcher = DATA_IMAGE_PATTERN.matcher(trimmed);
        if (!matcher.matches()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "A imagem deve ser PNG, JPEG ou WebP válida ou um caminho em media/pictures."
            );
        }

        String subtype = matcher.group(1).toLowerCase(Locale.ROOT);
        String base64Payload = matcher.group(2).replaceAll("\\s+", "");
        byte[] bytes;
        try {
            bytes = Base64.getMimeDecoder().decode(base64Payload);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Conteúdo da imagem inválido.");
        }

        if (bytes.length == 0 || bytes.length > MAX_PICTURE_BYTES) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "A imagem deve ter no máximo 5 MB."
            );
        }

        String extension = switch (subtype) {
            case "jpeg", "jpg" -> "jpg";
            case "webp" -> "webp";
            default -> "png";
        };

        String fileName = buildFileName(userId, extension);
        try {
            Files.createDirectories(storageDirectory);
            Path target = storageDirectory.resolve(fileName).normalize();
            if (!target.startsWith(storageDirectory)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Caminho de imagem inválido.");
            }
            Files.write(target, bytes);
        } catch (IOException e) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Não foi possível armazenar a foto de perfil no servidor."
            );
        }

        deleteFileIfPresent(existingPath);
        return PICTURES_DB_PREFIX + fileName;
    }

    /**
     * Recebe um MultipartFile enviado via upload multipart, salva em media/pictures e retorna "media/pictures/<arquivo>".
     */
    public String storeMultipartPicture(Long userId, MultipartFile file, String existingPath) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Selecione um arquivo de imagem.");
        }
        if (file.getSize() > MAX_PICTURE_BYTES) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A imagem deve ter no máximo 5 MB.");
        }

        String extension = resolveExtension(file.getContentType(), file.getOriginalFilename());
        String fileName = buildFileName(userId, extension);

        try {
            Files.createDirectories(storageDirectory);
            Path target = storageDirectory.resolve(fileName).normalize();
            if (!target.startsWith(storageDirectory)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Caminho de imagem inválido.");
            }
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Não foi possível salvar a foto de perfil."
            );
        }

        deleteFileIfPresent(existingPath);
        return PICTURES_DB_PREFIX + fileName;
    }

    public StoredPicture loadPicture(String requestedFileName) {
        String safeFileName = sanitizeFileName(requestedFileName);
        Path target = storageDirectory.resolve(safeFileName).normalize();
        if (!target.startsWith(storageDirectory) || !Files.exists(target) || !Files.isRegularFile(target)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Imagem não encontrada.");
        }
        try {
            byte[] bytes = Files.readAllBytes(target);
            MediaType mediaType = resolveMediaType(safeFileName);
            return new StoredPicture(bytes, mediaType, safeFileName);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erro ao ler imagem de perfil.");
        }
    }

    public void deleteFileIfPresent(String existingPath) {
        if (existingPath == null || existingPath.isBlank()) {
            return;
        }
        String normalized = existingPath.trim();
        if (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        if (!normalized.startsWith(PICTURES_DB_PREFIX)) {
            return;
        }
        String fileName = normalized.substring(PICTURES_DB_PREFIX.length());
        if (fileName.isBlank() || fileName.contains("..") || fileName.contains("/") || fileName.contains("\\")) {
            return;
        }
        try {
            Path target = storageDirectory.resolve(fileName).normalize();
            if (target.startsWith(storageDirectory)) {
                Files.deleteIfExists(target);
            }
        } catch (IOException ignored) {
        }
    }

    private String buildFileName(Long userId, String extension) {
        String prefix = userId == null ? "user" : "user_" + userId;
        String token = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        return prefix + "_" + token + "." + extension;
    }

    private String sanitizeFileName(String rawName) {
        if (rawName == null || rawName.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nome de arquivo inválido.");
        }
        if (rawName.contains("..") || rawName.contains("/") || rawName.contains("\\")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nome de arquivo inválido.");
        }
        String fileName = Paths.get(rawName).getFileName().toString();
        String lower = fileName.toLowerCase(Locale.ROOT);
        if (!(lower.endsWith(".png") || lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".webp"))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Formato de imagem não suportado.");
        }
        return fileName;
    }

    private String resolveExtension(String contentType, String originalFilename) {
        if (contentType != null) {
            String lowerType = contentType.toLowerCase(Locale.ROOT);
            if (lowerType.equals("image/png")) return "png";
            if (lowerType.equals("image/jpeg") || lowerType.equals("image/jpg")) return "jpg";
            if (lowerType.equals("image/webp")) return "webp";
        }
        if (originalFilename != null) {
            String lowerName = originalFilename.toLowerCase(Locale.ROOT);
            if (lowerName.endsWith(".png")) return "png";
            if (lowerName.endsWith(".jpg") || lowerName.endsWith(".jpeg")) return "jpg";
            if (lowerName.endsWith(".webp")) return "webp";
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A imagem deve ser PNG, JPEG ou WebP válida.");
    }

    private MediaType resolveMediaType(String fileName) {
        String lower = fileName.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".png")) return MediaType.IMAGE_PNG;
        if (lower.endsWith(".webp")) return MediaType.parseMediaType("image/webp");
        return MediaType.IMAGE_JPEG;
    }

    public record StoredPicture(byte[] bytes, MediaType mediaType, String fileName) {}
}
