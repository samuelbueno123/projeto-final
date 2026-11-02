package com.cefet_rj.projeto_final.users.controllers;

import com.cefet_rj.projeto_final.users.services.PictureStorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/media/pictures")
@RequiredArgsConstructor
@Tag(name = "Media Pictures", description = "Serviço de imagens armazenadas no servidor separadas por pastas")
public class MediaPictureController {

    private final PictureStorageService pictureStorageService;

    @GetMapping("/{category}/{filename:.+}")
    @Operation(summary = "Obtém uma imagem por pasta de categoria (ex: profiles/ ou condominiums/)")
    public ResponseEntity<byte[]> getPictureWithCategory(
            @PathVariable String category,
            @PathVariable String filename
    ) {
        return serve(category + "/" + filename);
    }

    @GetMapping("/{filename:.+}")
    @Operation(summary = "Obtém uma imagem na raiz de fotos ou por compatibilidade legada")
    public ResponseEntity<byte[]> getPictureLegacy(@PathVariable String filename) {
        return serve(filename);
    }

    private ResponseEntity<byte[]> serve(String relativePath) {
        PictureStorageService.StoredPicture picture = pictureStorageService.loadPicture(relativePath);
        return ResponseEntity.ok()
                .contentType(picture.mediaType())
                .cacheControl(CacheControl.maxAge(1, TimeUnit.HOURS).cachePublic())
                .body(picture.bytes());
    }
}
