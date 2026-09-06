package com.challenge.order.controller;

import com.challenge.order.dto.OrderPageResponse;
import com.challenge.order.dto.OrderResponse;
import com.challenge.order.exception.GlobalExceptionHandler;
import com.challenge.order.exception.OrderEventPublishException;
import com.challenge.order.exception.OrderNotFoundException;
import com.challenge.order.model.OrderStatus;
import com.challenge.order.service.OrderService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class OrderControllerTest {

    @Mock
    private OrderService orderService;

    @InjectMocks
    private OrderController controller;

    private MockMvc mockMvc;

    @Test
    void findByIdReturnsPublicOrderWithoutCiphertext() throws Exception {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        when(orderService.findById(1L)).thenReturn(new OrderResponse(1L, "Teclado", 1,
                BigDecimal.TEN, OrderStatus.PENDIENTE, now, now));

        mockMvc.perform(get("/api/orders/1").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.encryptedCardData").doesNotExist());
    }

    @Test
    void findByIdReturnsNotFoundError() throws Exception {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        when(orderService.findById(99L)).thenThrow(new OrderNotFoundException(99L));

        mockMvc.perform(get("/api/orders/99").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("No se encontró la orden 99"));
    }

    @Test
    void createReturnsServiceUnavailableWhenEventCannotBePublished() throws Exception {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        when(orderService.create(org.mockito.ArgumentMatchers.any()))
                .thenThrow(new OrderEventPublishException(new IllegalStateException("Kafka no disponible")));

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "productName": "Teclado",
                                  "quantity": 1,
                                  "amount": 10.00,
                                  "encryptedCardData": "cipher"
                                }
                                """))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value(503))
                .andExpect(jsonPath("$.message").value("No fue posible registrar la orden en este momento"));
    }
}
