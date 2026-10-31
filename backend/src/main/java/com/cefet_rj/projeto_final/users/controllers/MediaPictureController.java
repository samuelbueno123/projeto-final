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
@Tag(name = "Media Pictures", description = "Serviço de arquivos de fotos de perfil em media/pictures")
public class MediaPictureController {

    private final PictureStorageService pictureStorageService;

    @GetMapping("/{filename:.+}")
    @Operation(summary = "Obtém uma foto de perfil armazenada no servidor em media/pictures")
    public ResponseEntity<byte[]> getPicture(@PathVariable String filename) {
        PictureStorageService.StoredPicture picture = pictureStorageService.loadPicture(filename);
        return ResponseEntity.ok()
                .contentType(picture.mediaType())
                .cacheControl(CacheControl.maxAge(1, TimeUnit.HOURS).cachePublic())
                .body(picture.bytes());
    }
}
