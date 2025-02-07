package com.taskmanager.enums;

public enum TaskPriority {
    LOW("Low"),
    MEDIUM("Middle"),
    HIGH("High"),
    URGENT("Emergency");
    
    private final String displayValue;
    
    TaskPriority(String displayValue) {
        this.displayValue = displayValue;
    }
    
    public String getDisplayValue() {
        return displayValue;
    }
} 