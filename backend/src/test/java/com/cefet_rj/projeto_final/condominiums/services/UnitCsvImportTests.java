package com.cefet_rj.projeto_final.condominiums.services;

import com.cefet_rj.projeto_final.billing.entities.BillingPlan;
import com.cefet_rj.projeto_final.billing.repositories.BillingPlanRepository;
import com.cefet_rj.projeto_final.condominiums.dtos.UnitCsvImportResultDTO;
import com.cefet_rj.projeto_final.condominiums.entities.Condominium;
import com.cefet_rj.projeto_final.condominiums.repositories.CondominiumRepository;
import com.cefet_rj.projeto_final.condominiums.repositories.UnitRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class UnitCsvImportTests {

    @Autowired
    private UnitService unitService;

    @Autowired
    private CondominiumRepository condominiumRepository;

    @Autowired
    private BillingPlanRepository billingPlanRepository;

    @Autowired
    private UnitRepository unitRepository;

    @Test
    void testImportCsv_Success() {
        BillingPlan plan = new BillingPlan();
        plan.setName("Plano Teste");
        plan.setMonthlyFee(BigDecimal.valueOf(199.90));
        plan.setActive(true);
        plan = billingPlanRepository.save(plan);

        Condominium condo = new Condominium();
        condo.setName("Condomínio Teste CSV");
        condo.setAddress("Rua Teste, 100");
        condo.setBillingPlan(plan);
        condo.setIsActive(true);
        condo = condominiumRepository.save(condo);

        String csvData = "setor,identificador\n" +
                "Bloco A,101\n" +
                "Bloco A,102\n" +
                "Bloco B,201\n";

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "unidades.csv",
                "text/csv",
                csvData.getBytes(StandardCharsets.UTF_8)
        );

        UnitCsvImportResultDTO result = unitService.importCsv(condo.getId(), file);

        Assertions.assertEquals(3, result.totalProcessed());
        Assertions.assertEquals(3, result.importedCount());
        Assertions.assertEquals(0, result.skippedCount());
        Assertions.assertEquals(3, unitRepository.findAllForCondominium(condo.getId()).size());
    }
}
