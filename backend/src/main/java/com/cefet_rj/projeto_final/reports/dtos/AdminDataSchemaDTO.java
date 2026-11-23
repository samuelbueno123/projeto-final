package com.cefet_rj.projeto_final.reports.dtos;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Schema and governed mutation contracts for the structured Admin DB Record Editor.
 */
public final class AdminDataSchemaDTO {

    private AdminDataSchemaDTO() {
    }

    public record AdminResourceMetaDTO(
            String resource,
            String label,
            String category,
            boolean creatable,
            boolean editable,
            boolean deletable,
            boolean immutableLedger,
            String description
    ) {}

    public record EnumOption(
            String value,
            String label
    ) {}

    public record FieldDescriptor(
            String name,
            String label,
            String type,
            boolean required,
            boolean editable,
            boolean immutableOnUpdate,
            boolean hidden,
            Integer maxLength,
            List<EnumOption> enumOptions,
            String relationResource,
            String relationDisplayField,
            String helperText,
            Object defaultValue
    ) {}

    public record ResourceSchemaDTO(
            String resource,
            String label,
            boolean creatable,
            boolean editable,
            boolean deletable,
            boolean requiresReason,
            boolean immutableLedger,
            Set<String> protectedFields,
            List<FieldDescriptor> fields
    ) {}

    public record AdminRecordMutationRequest(
            Map<String, Object> data,
            String reason
    ) {}

    public record AdminRecordMutationResponse(
            String resource,
            Long id,
            String action,
            String reason,
            Map<String, Object> record,
            Map<String, Object> beforeState,
            Map<String, Object> afterState
    ) {}
}
