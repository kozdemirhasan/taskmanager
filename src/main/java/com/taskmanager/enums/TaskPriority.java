package com.taskmanager.enums;

public enum TaskPriority {
    LOW("Düşük"),
    MEDIUM("Orta"),
    HIGH("Yüksek"),
    URGENT("Acil");
    
    private final String displayValue;
    
    TaskPriority(String displayValue) {
        this.displayValue = displayValue;
    }
    
    public String getDisplayValue() {
        return displayValue;
    }
} 