package com.cefet_rj.projeto_final.auth.security;

import com.nimbusds.jose.jwk.RSAKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

@Service
public class PersistentRsaKeyService {

    private final Path privateKeyPath;
    private final Path publicKeyPath;

    public PersistentRsaKeyService(
            @Value("${security.jwt.private-key-path}") String privateKeyPath,
            @Value("${security.jwt.public-key-path}") String publicKeyPath
    ) {
        this.privateKeyPath = Path.of(privateKeyPath);
        this.publicKeyPath = Path.of(publicKeyPath);
    }

    public RSAKey loadOrCreate() {

        try {

            if (Files.exists(privateKeyPath)
                    && Files.exists(publicKeyPath)) {

                return loadExistingKey();
            }

            return createAndPersistKey();

        } catch (Exception ex) {

            throw new IllegalStateException(
                    "Could not load or create RSA signing key",
                    ex
            );
        }
    }

    private RSAKey loadExistingKey() throws Exception {

        RSAPrivateKey privateKey =
                readPrivateKey();

        RSAPublicKey publicKey =
                readPublicKey();

        return buildRsaKey(
                publicKey,
                privateKey
        );
    }

    private RSAKey createAndPersistKey() throws Exception {

        KeyPairGenerator generator =
                KeyPairGenerator.getInstance("RSA");

        generator.initialize(2048);

        KeyPair keyPair =
                generator.generateKeyPair();

        RSAPublicKey publicKey =
                (RSAPublicKey) keyPair.getPublic();

        RSAPrivateKey privateKey =
                (RSAPrivateKey) keyPair.getPrivate();

        createParentDirectories();

        writePem(
                privateKeyPath,
                "PRIVATE KEY",
                privateKey.getEncoded()
        );

        writePem(
                publicKeyPath,
                "PUBLIC KEY",
                publicKey.getEncoded()
        );

        return buildRsaKey(
                publicKey,
                privateKey
        );
    }

    private RSAKey buildRsaKey(
            RSAPublicKey publicKey,
            RSAPrivateKey privateKey
    ) throws Exception {

        return new RSAKey.Builder(publicKey)
                .privateKey(privateKey)
                .keyID(generateKeyId(publicKey))
                .build();
    }

    private String generateKeyId(
            RSAPublicKey publicKey
    ) throws Exception {

        MessageDigest digest =
                MessageDigest.getInstance("SHA-256");

        byte[] hash =
                digest.digest(
                        publicKey.getEncoded()
                );

        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(hash);
    }

    private RSAPrivateKey readPrivateKey()
            throws Exception {

        String pem =
                Files.readString(
                        privateKeyPath,
                        StandardCharsets.UTF_8
                );

        byte[] encoded =
                decodePem(
                        pem,
                        "PRIVATE KEY"
                );

        KeyFactory keyFactory =
                KeyFactory.getInstance("RSA");

        return (RSAPrivateKey)
                keyFactory.generatePrivate(
                        new PKCS8EncodedKeySpec(encoded)
                );
    }

    private RSAPublicKey readPublicKey()
            throws Exception {

        String pem =
                Files.readString(
                        publicKeyPath,
                        StandardCharsets.UTF_8
                );

        byte[] encoded =
                decodePem(
                        pem,
                        "PUBLIC KEY"
                );

        KeyFactory keyFactory =
                KeyFactory.getInstance("RSA");

        return (RSAPublicKey)
                keyFactory.generatePublic(
                        new X509EncodedKeySpec(encoded)
                );
    }

    private byte[] decodePem(
            String pem,
            String type
    ) {

        String normalized =
                pem.replace(
                                "-----BEGIN " + type + "-----",
                                ""
                        )
                        .replace(
                                "-----END " + type + "-----",
                                ""
                        )
                        .replaceAll("\\s", "");

        return Base64.getDecoder()
                .decode(normalized);
    }

    private void writePem(
            Path path,
            String type,
            byte[] encoded
    ) throws IOException {

        String content =
                "-----BEGIN " + type + "-----\n"
                        + Base64.getMimeEncoder(
                                64,
                                "\n".getBytes(
                                        StandardCharsets.UTF_8
                                )
                        )
                        .encodeToString(encoded)
                        + "\n-----END " + type + "-----\n";

        Files.writeString(
                path,
                content,
                StandardCharsets.UTF_8
        );
    }

    private void createParentDirectories()
            throws IOException {

        if (privateKeyPath.getParent() != null) {
            Files.createDirectories(
                    privateKeyPath.getParent()
            );
        }

        if (publicKeyPath.getParent() != null) {
            Files.createDirectories(
                    publicKeyPath.getParent()
            );
        }
    }
}