package com.cefet_rj.projeto_final.manager.dtos;

import com.cefet_rj.projeto_final.manager.enums.UnitPaymentStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

public record UnitPaymentUpdateDTO(@NotNull UnitPaymentStatus status, LocalDate dueDate,
                                  @DecimalMin("0.0") BigDecimal amount) {}
