package com.cefet_rj.projeto_final.condominiums.dtos;

import java.util.List;

public record UnitCsvImportResultDTO(
        int totalProcessed,
        int importedCount,
        int skippedCount,
        List<String> errors
) {}
