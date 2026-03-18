package com.example.order.service.client;

public class UpdateStockRequest {
    private int change;

    public UpdateStockRequest(int change) {
        this.change = change;
    }

    public int getChange() {
        return change;
    }

    public void setChange(int change) {
        this.change = change;
    }
}