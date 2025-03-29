package org.horikita.dto;

public enum OrderStatusEnum {
    PLACED("placed"),
    PENDING("pending"),
    EXECUTED("executed"),
    ERROR("error");

    private final String status;

    OrderStatusEnum(String status) {
        this.status = status;
    }

    public String getStatus() {
        return status;
    }
}
