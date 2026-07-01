package com.cefet_rj.projeto_final.packages.projections;

import com.cefet_rj.projeto_final.shared.projections.IdProjection;

public interface PackageProjection extends IdProjection<Long> {
    Long getId();
}