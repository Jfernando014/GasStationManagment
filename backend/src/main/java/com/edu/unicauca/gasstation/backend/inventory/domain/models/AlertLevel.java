package com.edu.unicauca.gasstation.backend.inventory.domain.models;

/**
 * Level of a variation alert. REVISAR, AYUDA and ALARMA are the CHECK, HELP and ALARM of the Excel; OK means
 * that there is no alert. The shortage ranges only use OK, REVISAR and ALARMA.
 */
public enum AlertLevel {
    OK,
    REVISAR,
    AYUDA,
    ALARMA
}
