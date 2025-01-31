package com.taskmanager.enums;

public enum TaskStatus {
    PENDING("Beklemede"),
    COMPLETED("Tamamlandı"),
    CANCELLED("İptal Edildi");
    
    private final String displayValue;
    
    TaskStatus(String displayValue) {
        this.displayValue = displayValue;
    }
    
    public String getDisplayValue() {
        return displayValue;
    }
} 