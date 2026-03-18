package com.example.notification.dto.response;

import lombok.Data;

@Data
public class ApiResponRequest<T> {
    private int code;
    private String message;
    private T result;

    public static <T> Builder<T> builder() {
        return new Builder<>();
    }

    public static class Builder<T> {
        private int code;
        private String message;
        private T result;

        public Builder<T> code(int code) {
            this.code = code;
            return this;
        }

        public Builder<T> message(String message) {
            this.message = message;
            return this;
        }

        public Builder<T> result(T result) {
            this.result = result;
            return this;
        }

        public ApiResponRequest<T> build() {
            ApiResponRequest<T> response = new ApiResponRequest<>();
            response.setCode(code);
            response.setMessage(message);
            response.setResult(result);
            return response;
        }
    }
}