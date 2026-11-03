package com.cefet_rj.projeto_final.assemblies.repositories;

import com.cefet_rj.projeto_final.assemblies.entities.AssemblyVote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AssemblyVoteRepository extends JpaRepository<AssemblyVote, Long> {

    Optional<AssemblyVote> findByTopicIdAndUserId(Long topicId, Long userId);

    @Query("SELECT v FROM AssemblyVote v WHERE v.topic.id IN :topicIds AND v.user.id = :userId")
    List<AssemblyVote> findAllByTopicIdsAndUserId(@Param("topicIds") List<Long> topicIds, @Param("userId") Long userId);

    long countByTopicId(Long topicId);
}
