package com.cefet_rj.projeto_final.condominiums.repositories;

import com.cefet_rj.projeto_final.condominiums.entities.CondominiumGalleryImage;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CondominiumGalleryRepository extends JpaRepository<CondominiumGalleryImage, Long> {
    @EntityGraph(attributePaths = {"commonArea", "uploadedBy"})
    List<CondominiumGalleryImage> findByCondominiumIdOrderByCreatedAtDesc(Long condominiumId);

    @EntityGraph(attributePaths = {"commonArea", "uploadedBy"})
    List<CondominiumGalleryImage> findByCommonAreaIdOrderByCreatedAtDesc(Long commonAreaId);
}
