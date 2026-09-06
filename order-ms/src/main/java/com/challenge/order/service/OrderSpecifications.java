package com.challenge.order.service;

import com.challenge.order.model.Order;
import com.challenge.order.model.OrderStatus;
import org.springframework.data.jpa.domain.Specification;

import java.util.Locale;

final class OrderSpecifications {

    private OrderSpecifications() {
    }

    static Specification<Order> withFilters(OrderStatus status, String productName) {
        Specification<Order> specification = (root, query, criteriaBuilder) ->
                criteriaBuilder.conjunction();
        if (status != null) {
            specification = specification.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.equal(root.get("status"), status));
        }
        if (productName != null && !productName.isBlank()) {
            String pattern = "%" + productName.trim().toLowerCase(Locale.ROOT) + "%";
            specification = specification.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.like(
                            criteriaBuilder.lower(root.get("productName")),
                            pattern
                    ));
        }
        return specification;
    }
}
