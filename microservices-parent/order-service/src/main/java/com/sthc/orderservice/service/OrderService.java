package com.sthc.orderservice.service;

import com.sthc.orderservice.dto.InventoryResponse;
import com.sthc.orderservice.dto.OrderLineItemsdto;
import com.sthc.orderservice.dto.OrderRequest;
import com.sthc.orderservice.model.Order;
import com.sthc.orderservice.model.OrderLineItems;
import com.sthc.orderservice.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class OrderService {

    private final OrderRepository orderRepository;
    private final WebClient.Builder webClientBuilder;
    public  void placeOrder(OrderRequest orderRequest){
        Order order = new Order();
        order.setOrderNumber(UUID.randomUUID().toString());

        List<OrderLineItems> orderLineItems = orderRequest.getOrderLineItemsdtoList()
                .stream()
                .map(this::mapToDto)
                .toList();

        order.setOrderLineItemsList(orderLineItems);

        List<String> skuCodes = order.getOrderLineItemsList().stream()
                .map(OrderLineItems::getSkuCode)
                .toList();

        //Call Inventory service,and place order if product is in stock
        InventoryResponse[] inventoryResponseArray = webClientBuilder.build().get()
                        .uri("http://inventory-service/api/inventory",
                                uriBuilder->uriBuilder.queryParam("skuCode",skuCodes).build())
                                .retrieve()
                                        .bodyToMono(InventoryResponse[].class)
                                                .block();
        //problem need to fix
        boolean allMatch = Arrays.stream(inventoryResponseArray)
                .allMatch(InventoryResponse::getIsInStock);

        if (allMatch){
            orderRepository.save(order);
        }
        else{
            throw  new IllegalArgumentException("Product is not in stock,please try later");
        }



    }

    private OrderLineItems mapToDto(OrderLineItemsdto orderLineItemsdto) {
        OrderLineItems orderLineItems = new OrderLineItems();

        orderLineItems.setPrice(orderLineItemsdto.getPrice());
        orderLineItems.setQuantity(orderLineItemsdto.getQuantity());
        orderLineItems.setPrice(orderLineItemsdto.getPrice());

        return orderLineItems;
    }
}
