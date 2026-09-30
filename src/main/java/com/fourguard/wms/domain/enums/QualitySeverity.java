package com.fourguard.wms.domain.enums;

public enum QualitySeverity {
    CRITICAL("RED"),
    WARNING("YELLOW"),
    INFO("BLUE");

    private final String dbValue;

    QualitySeverity(String dbValue) {
        this.dbValue = dbValue;
    }

    public String getDbValue() {
        return dbValue;
    }

    public static QualitySeverity fromDb(String db) {
        if (db == null) return INFO;
        if ("RED".equalsIgnoreCase(db)) return CRITICAL;
        if ("YELLOW".equalsIgnoreCase(db)) return WARNING;
        return INFO;
    }
}
