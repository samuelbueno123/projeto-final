package com.cefet_rj.projeto_final.community.repositories;

import com.cefet_rj.projeto_final.community.entities.EventGuestList;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EventGuestListRepository extends JpaRepository<EventGuestList, Long> {
    List<EventGuestList> findByCondominiumIdOrderByEventDateDesc(Long condominiumId);
    List<EventGuestList> findByCondominiumIdAndHostIdOrderByEventDateDesc(Long condominiumId, Long hostId);
}
