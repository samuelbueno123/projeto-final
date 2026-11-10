package com.cefet_rj.projeto_final.community.repositories;

import com.cefet_rj.projeto_final.community.entities.PollVote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PollVoteRepository extends JpaRepository<PollVote, Long> {
    List<PollVote> findByPollId(Long pollId);
    Optional<PollVote> findByPollIdAndVoterId(Long pollId, Long voterId);

    @Query("SELECT v FROM PollVote v JOIN FETCH v.option JOIN FETCH v.voter WHERE v.poll.condominium.id = :condoId")
    List<PollVote> findAllByCondominiumIdWithDetails(@Param("condoId") Long condoId);
}
