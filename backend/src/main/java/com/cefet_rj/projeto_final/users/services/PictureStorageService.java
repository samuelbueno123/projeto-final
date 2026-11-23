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
    public static final String PROFILES_FOLDER = "profiles";
    public static final String CONDOMINIUMS_FOLDER = "condominiums";
    public static final String ANNOUNCEMENTS_FOLDER = "announcements";

    private static final long MAX_PICTURE_BYTES = 10L * 1024L * 1024L; // 10 MB
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
            Files.createDirectories(this.storageDirectory.resolve(PROFILES_FOLDER));
            Files.createDirectories(this.storageDirectory.resolve(CONDOMINIUMS_FOLDER));
            Files.createDirectories(this.storageDirectory.resolve(ANNOUNCEMENTS_FOLDER));
        } catch (IOException e) {
            throw new IllegalStateException("Não foi possível criar o diretório de fotos: " + this.storageDirectory, e);
        }
    }

    /**
     * Persiste uma única cópia em disco para imagens de avisos globais em vez de duplicar
     * strings base64 em todas as linhas de tb_notification.
     */
    public String storeSystemAnnouncementImage(String inputImage) {
        return storeDecodedImage(ANNOUNCEMENTS_FOLDER, "sys", inputImage, null);
    }

    public Path getStorageDirectory() {
        return storageDirectory;
    }

    /**
     * Armazena foto de perfil de usuário na subpasta "profiles".
     */
    public String resolveOrStoreProfileImage(Long userId, String inputImage, String existingPath) {
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Identificador do usuário é obrigatório.");
        }
        return storeDecodedImage(
                PROFILES_FOLDER,
                "user_" + userId,
                inputImage,
                existingPath
        );
    }

    /**
     * Armazena imagem do condomínio ou área comum na subpasta "condominiums".
     */
    public String resolveOrStoreCondominiumImage(Long condominiumId, String inputImage, String existingPath) {
        if (condominiumId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Identificador do condomínio é obrigatório.");
        }
        return storeDecodedImage(
                CONDOMINIUMS_FOLDER,
                "condo_" + condominiumId,
                inputImage,
                existingPath
        );
    }

    private String storeDecodedImage(String folder, String prefix, String inputImage, String existingPath) {
        if (inputImage == null || inputImage.isBlank()) {
            deleteFileIfPresent(existingPath);
            return null;
        }

        String trimmed = inputImage.trim();

        if (trimmed.startsWith(PICTURES_DB_PREFIX) || trimmed.startsWith("/" + PICTURES_DB_PREFIX)) {
            String normalized = trimmed.startsWith("/") ? trimmed.substring(1) : trimmed;
            String relative = normalized.substring(PICTURES_DB_PREFIX.length());
            validateSafeRelativePath(relative);
            String fullNormalized = PICTURES_DB_PREFIX + relative;
            if (existingPath != null && existingPath.equals(fullNormalized)) {
                return fullNormalized;
            }
            if (!relative.startsWith(folder + "/" + prefix + "_") && !relative.startsWith(prefix + "_")) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Caminho de imagem não pertence a este recurso.");
            }
            return fullNormalized;
        }

        Matcher matcher = DATA_IMAGE_PATTERN.matcher(trimmed);
        if (!matcher.matches()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "A imagem deve ser PNG, JPEG ou WebP válida ou um caminho existente em media/pictures."
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
                    "A imagem deve ter no máximo 10 MB."
            );
        }

        String extension = switch (subtype) {
            case "jpeg", "jpg" -> "jpg";
            case "webp" -> "webp";
            default -> "png";
        };
        validateImageMagicBytes(bytes, extension);

        String token = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        String fileName = prefix + "_" + token + "." + extension;
        String relativePath = folder + "/" + fileName;

        try {
            Path targetFolder = storageDirectory.resolve(folder);
            Files.createDirectories(targetFolder);
            Path target = targetFolder.resolve(fileName).normalize();
            if (!target.startsWith(storageDirectory)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Caminho de imagem inválido.");
            }
            Files.write(target, bytes);
        } catch (IOException e) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Não foi possível armazenar a imagem no servidor."
            );
        }

        deleteFileIfPresent(existingPath);
        return PICTURES_DB_PREFIX + relativePath;
    }

    /**
     * Recebe um MultipartFile enviado via upload multipart, salva na subpasta especificada.
     */
    public String storeMultipartPicture(String folder, String prefix, MultipartFile file, String existingPath) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Selecione um arquivo de imagem.");
        }
        if (file.getSize() > MAX_PICTURE_BYTES) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A imagem deve ter no máximo 10 MB.");
        }

        String extension = resolveExtension(file.getContentType(), file.getOriginalFilename());
        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Não foi possível ler o arquivo de imagem enviado.");
        }
        if (bytes.length == 0 || bytes.length > MAX_PICTURE_BYTES) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A imagem deve ter no máximo 10 MB.");
        }
        validateImageMagicBytes(bytes, extension);

        String token = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        String fileName = prefix + "_" + token + "." + extension;
        String relativePath = folder + "/" + fileName;

        try {
            Path targetFolder = storageDirectory.resolve(folder);
            Files.createDirectories(targetFolder);
            Path target = targetFolder.resolve(fileName).normalize();
            if (!target.startsWith(storageDirectory)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Caminho de imagem inválido.");
            }
            Files.write(target, bytes);
        } catch (IOException e) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Não foi possível salvar a imagem no servidor."
            );
        }

        deleteFileIfPresent(existingPath);
        return PICTURES_DB_PREFIX + relativePath;
    }

    private void validateImageMagicBytes(byte[] bytes, String expectedExtension) {
        boolean isPng = bytes.length >= 8
                && (bytes[0] & 0xFF) == 0x89
                && bytes[1] == 0x50
                && bytes[2] == 0x4E
                && bytes[3] == 0x47
                && bytes[4] == 0x0D
                && bytes[5] == 0x0A
                && bytes[6] == 0x1A
                && bytes[7] == 0x0A;
        boolean isJpeg = bytes.length >= 3
                && (bytes[0] & 0xFF) == 0xFF
                && (bytes[1] & 0xFF) == 0xD8
                && (bytes[2] & 0xFF) == 0xFF;
        boolean isWebp = bytes.length >= 12
                && bytes[0] == 'R' && bytes[1] == 'I' && bytes[2] == 'F' && bytes[3] == 'F'
                && bytes[8] == 'W' && bytes[9] == 'E' && bytes[10] == 'B' && bytes[11] == 'P';

        boolean matches = switch (expectedExtension) {
            case "png" -> isPng;
            case "jpg" -> isJpeg;
            case "webp" -> isWebp;
            default -> false;
        };
        if (!matches) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "O conteúdo do arquivo não corresponde a uma imagem PNG, JPEG ou WebP válida."
            );
        }
    }

    public StoredPicture loadPicture(String requestedPath) {
        validateSafeRelativePath(requestedPath);
        Path target = storageDirectory.resolve(requestedPath).normalize();
        if (!target.startsWith(storageDirectory) || !Files.exists(target) || !Files.isRegularFile(target)) {
            // Tentativa de compatibilidade legada se foi passado sem a subpasta
            Path legacyProfile = storageDirectory.resolve(PROFILES_FOLDER).resolve(requestedPath).normalize();
            if (legacyProfile.startsWith(storageDirectory) && Files.exists(legacyProfile) && Files.isRegularFile(legacyProfile)) {
                target = legacyProfile;
            } else {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Imagem não encontrada.");
            }
        }
        try {
            byte[] bytes = Files.readAllBytes(target);
            MediaType mediaType = resolveMediaType(target.getFileName().toString());
            return new StoredPicture(bytes, mediaType, target.getFileName().toString());
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erro ao ler imagem.");
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
        String relative = normalized.substring(PICTURES_DB_PREFIX.length());
        if (relative.isBlank() || relative.contains("..")) {
            return;
        }
        try {
            Path target = storageDirectory.resolve(relative).normalize();
            if (target.startsWith(storageDirectory)) {
                Files.deleteIfExists(target);
            }
        } catch (IOException ignored) {
        }
    }

    private void validateSafeRelativePath(String path) {
        if (path == null || path.isBlank() || path.contains("..") || path.contains("\\")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Caminho de imagem inválido.");
        }
        String lower = path.toLowerCase(Locale.ROOT);
        if (!(lower.endsWith(".png") || lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".webp"))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Formato de imagem não suportado.");
        }
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
