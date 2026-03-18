package com.example.order.dto;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;

@Data
public class AddressDTO {
    @NotBlank(message = "Address line 1 is required")
    private String addressLine1;
    private String addressLine2;
     @NotBlank(message = "City is required")
    private String city;
    @NotBlank(message = "Postal code is required")
    private String postalCode;
    @NotBlank(message = "Country is required")
    private String country;
}