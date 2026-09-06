package com.challenge.order.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CreateOrderRequest(
        @NotBlank(message = "El producto es obligatorio")
        @Size(max = 100, message = "El producto no puede superar 100 caracteres")
        String productName,

        @NotNull(message = "La cantidad es obligatoria")
        @Positive(message = "La cantidad debe ser mayor que cero")
        Integer quantity,

        @NotNull(message = "El monto es obligatorio")
        @DecimalMin(value = "0.00", inclusive = true, message = "El monto debe ser mayor o igual que cero")
        BigDecimal amount,

        @NotBlank(message = "Los datos cifrados de tarjeta son obligatorios")
        String encryptedCardData
) {
}
