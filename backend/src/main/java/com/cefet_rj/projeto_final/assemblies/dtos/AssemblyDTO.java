package com.cefet_rj.projeto_final.assemblies.dtos;

import com.cefet_rj.projeto_final.assemblies.entities.AssemblyTopic;
import com.cefet_rj.projeto_final.assemblies.entities.CondominiumAssembly;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

public record AssemblyDTO(
        Long id,
        Long condominiumId,
        String title,
        String description,
        String type,
        LocalDateTime meetingDate,
        LocalDateTime secondCallDate,
        String locationOrLink,
        String callNoticeUrl,
        String minutesUrl,
        String status,
        String createdByName,
        LocalDateTime createdAt,
        List<AssemblyTopicDTO> topics
) {
    public AssemblyDTO(CondominiumAssembly entity) {
        this(
                entity.getId(),
                entity.getCondominium().getId(),
                entity.getTitle(),
                entity.getDescription(),
                entity.getType(),
                entity.getMeetingDate(),
                entity.getSecondCallDate(),
                entity.getLocationOrLink(),
                entity.getCallNoticeUrl(),
                entity.getMinutesUrl(),
                entity.getStatus(),
                entity.getCreatedBy() != null
                        ? (entity.getCreatedBy().getFirstName() + " " + entity.getCreatedBy().getLastName()).trim()
                        : "Administração",
                entity.getCreatedAt(),
                entity.getTopics() != null
                        ? entity.getTopics().stream().map(AssemblyTopicDTO::new).toList()
                        : Collections.emptyList()
        );
    }

    public record AssemblyTopicDTO(
            Long id,
            Long assemblyId,
            String title,
            String description,
            boolean votingOpen,
            int displayOrder,
            int inFavorCount,
            int againstCount,
            int abstainCount,
            String userVote
    ) {
        public AssemblyTopicDTO(AssemblyTopic topic) {
            this(
                    topic.getId(),
                    topic.getAssembly().getId(),
                    topic.getTitle(),
                    topic.getDescription(),
                    topic.isVotingOpen(),
                    topic.getDisplayOrder(),
                    topic.getInFavorCount(),
                    topic.getAgainstCount(),
                    topic.getAbstainCount(),
                    null
            );
        }

        public AssemblyTopicDTO withUserVote(String vote) {
            return new AssemblyTopicDTO(
                    id(),
                    assemblyId(),
                    title(),
                    description(),
                    votingOpen(),
                    displayOrder(),
                    inFavorCount(),
                    againstCount(),
                    abstainCount(),
                    vote
            );
        }
    }

    public record CreateAssemblyDTO(
            @NotBlank String title,
            String description,
            String type,
            @NotNull LocalDateTime meetingDate,
            LocalDateTime secondCallDate,
            String locationOrLink,
            String callNoticeUrl
    ) {}

    public record CreateTopicDTO(
            @NotBlank String title,
            String description,
            Integer displayOrder
    ) {}

    public record CastVoteDTO(
            @NotBlank String voteChoice
    ) {}

    public record UpdateAssemblyStatusDTO(
            @NotBlank String status,
            String minutesUrl
    ) {}
}
