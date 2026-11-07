package com.cefet_rj.projeto_final.condominiums.services;

import com.cefet_rj.projeto_final.auth.security.AuthenticatedUserService;
import com.cefet_rj.projeto_final.condominiums.dtos.CondominiumGalleryDTO;
import com.cefet_rj.projeto_final.condominiums.entities.Condominium;
import com.cefet_rj.projeto_final.condominiums.entities.CondominiumGalleryImage;
import com.cefet_rj.projeto_final.condominiums.repositories.CondominiumGalleryRepository;
import com.cefet_rj.projeto_final.condominiums.repositories.CondominiumRepository;
import com.cefet_rj.projeto_final.reservations.entities.CommonArea;
import com.cefet_rj.projeto_final.reservations.repositories.CommonAreaRepository;
import com.cefet_rj.projeto_final.shared.exceptions.ResourceNotFoundException;
import com.cefet_rj.projeto_final.users.entities.User;
import com.cefet_rj.projeto_final.users.services.PictureStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CondominiumGalleryService {

    private final CondominiumGalleryRepository galleryRepository;
    private final CondominiumRepository condominiumRepository;
    private final CommonAreaRepository commonAreaRepository;
    private final PictureStorageService pictureStorageService;
    private final AuthenticatedUserService authenticatedUserService;
    private final com.cefet_rj.projeto_final.condominiums.security.CondominiumAccessService condominiumAccessService;

    @Transactional(readOnly = true)
    public List<CondominiumGalleryDTO> findByCondominium(Long condominiumId) {
        condominiumAccessService.validateAccess(condominiumId);
        return galleryRepository.findByCondominiumIdOrderByCreatedAtDesc(condominiumId).stream()
                .map(CondominiumGalleryDTO::new)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CondominiumGalleryDTO> findByCommonArea(Long condominiumId, Long commonAreaId) {
        condominiumAccessService.validateAccess(condominiumId);
        CommonArea area = commonAreaRepository.findById(commonAreaId)
                .orElseThrow(() -> new ResourceNotFoundException("Área comum não encontrada: " + commonAreaId));
        if (!area.getCondominium().getId().equals(condominiumId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Área comum não pertence a este condomínio.");
        }
        return galleryRepository.findByCommonAreaIdOrderByCreatedAtDesc(commonAreaId).stream()
                .map(CondominiumGalleryDTO::new)
                .toList();
    }

    @Transactional
    public CondominiumGalleryDTO addImage(Long condominiumId, CondominiumGalleryDTO.CreateGalleryImageDTO dto) {
        condominiumAccessService.validateManagerAccess(condominiumId);
        User user = authenticatedUserService.getAuthenticatedUser();
        Condominium condominium = condominiumRepository.findById(condominiumId)
                .orElseThrow(() -> new ResourceNotFoundException("Condomínio não encontrado: " + condominiumId));

        String storedPath = pictureStorageService.resolveOrStoreCondominiumImage(
                condominiumId,
                dto.image(),
                null
        );

        CondominiumGalleryImage image = new CondominiumGalleryImage();
        image.setCondominium(condominium);
        image.setTitle(dto.title().trim());
        image.setDescription(dto.description() == null ? null : dto.description().trim());
        image.setImagePath(storedPath);
        image.setUploadedBy(user);

        if (dto.commonAreaId() != null) {
            CommonArea area = commonAreaRepository.findById(dto.commonAreaId())
                    .orElseThrow(() -> new ResourceNotFoundException("Área comum não encontrada: " + dto.commonAreaId()));
            if (!area.getCondominium().getId().equals(condominiumId)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Área comum não pertence a este condomínio.");
            }
            image.setCommonArea(area);
            if (area.getImageUrl() == null || area.getImageUrl().isBlank()) {
                area.setImageUrl(storedPath);
                commonAreaRepository.save(area);
            }
        }

        image = galleryRepository.save(image);
        return new CondominiumGalleryDTO(image);
    }

    @Transactional
    public void deleteImage(Long condominiumId, Long imageId) {
        condominiumAccessService.validateManagerAccess(condominiumId);
        CondominiumGalleryImage image = galleryRepository.findById(imageId)
                .orElseThrow(() -> new ResourceNotFoundException("Imagem não encontrada: " + imageId));

        if (!image.getCondominium().getId().equals(condominiumId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Imagem não pertence a este condomínio.");
        }

        pictureStorageService.deleteFileIfPresent(image.getImagePath());
        galleryRepository.delete(image);
    }
}
