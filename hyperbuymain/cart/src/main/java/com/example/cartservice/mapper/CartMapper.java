package com.example.cartservice.mapper;

import com.example.cartservice.dto.response.CartResponse;
import com.example.cartservice.entity.Cart;
import com.example.cartservice.entity.CartItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

import java.math.BigDecimal;
import java.util.List;

@Mapper(componentModel = "spring", uses = {CartItemMapper.class})
public interface CartMapper {
    CartMapper INSTANCE = Mappers.getMapper(CartMapper.class);

    @Mapping(target = "totalUniqueItems", expression = "java(cart.getItems() != null ? cart.getItems().size() : 0)")
    @Mapping(target = "totalQuantity", source = "items", qualifiedByName = "calculateTotalQuantity")
    @Mapping(target = "grandTotal", source = "items", qualifiedByName = "calculateGrandTotal")
    CartResponse toCartResponse(Cart cart);

    List<CartResponse> toCartResponseList(List<Cart> carts);

    @Named("calculateTotalQuantity")
    default Integer calculateTotalQuantity(List<CartItem> items) {
        if (items == null) return 0;
        return items.stream().mapToInt(CartItem::getQuantity).sum();
    }

    @Named("calculateGrandTotal")
    default BigDecimal calculateGrandTotal(List<CartItem> items) {
        if (items == null) return BigDecimal.ZERO;
        return items.stream()
                .map(item -> item.getPriceAtAddition().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}