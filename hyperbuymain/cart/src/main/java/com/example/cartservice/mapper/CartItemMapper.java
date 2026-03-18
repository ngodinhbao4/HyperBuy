package com.example.cartservice.mapper;

import com.example.cartservice.dto.response.CartItemResponse;
import com.example.cartservice.dto.request.ProductDetailRequest;
import com.example.cartservice.entity.CartItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;


@Mapper(componentModel = "spring")
public interface CartItemMapper {
    CartItemMapper INSTANCE = Mappers.getMapper(CartItemMapper.class);

    @Mapping(target = "id", source = "cartItem.id")
    @Mapping(target = "productName", source = "productDetail.name")
    @Mapping(target = "imageUrl", source = "productDetail.imageUrl")
    @Mapping(target = "currentPrice", source = "productDetail.price")
    @Mapping(target = "lineItemTotal", expression = "java(cartItem.getPriceAtAddition().multiply(java.math.BigDecimal.valueOf(cartItem.getQuantity())))")
    CartItemResponse toCartItemResponse(CartItem cartItem, ProductDetailRequest productDetail);
}