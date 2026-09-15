package com.cefet_rj.projeto_final.condominiums.enums;

import com.cefet_rj.projeto_final.shared.exceptions.InvalidStatusException;

public enum SectorStructuralType {
    BLOCK,
    STREET,
    COURT,
    GRILL;

    public static SectorStructuralType fromString(String value) {
        for (SectorStructuralType type : SectorStructuralType.values()) {
            if (type.name().equalsIgnoreCase(value)) {
                return type;
            }
        }

        throw new InvalidStatusException("Invalid sector structural type: " + value);
    }
}