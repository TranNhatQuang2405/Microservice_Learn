package learn.microservice.service;

import learn.microservice.dto.InventoryResponse;
import learn.microservice.dto.OrderLineItemsDto;
import learn.microservice.dto.OrderRequest;
import learn.microservice.model.Order;
import learn.microservice.model.OrderLineItems;
import learn.microservice.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final WebClient.Builder webClientBuilder;

    public void placeOrder(OrderRequest orderRequest){
        Order order = new Order();
        order.setOrderNumber(UUID.randomUUID().toString());
        List<OrderLineItems> orderLineItems = orderRequest.getOrderLineItemsDtoList()
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());

        order.setOrderLineItemsList(orderLineItems);

        List<String> skuCodes = order.getOrderLineItemsList().stream().map(OrderLineItems::getSkuCode).collect(Collectors.toList());

        InventoryResponse[] inventoryResponses = webClientBuilder.build().get()
                .uri("http://inventory-service/api/inventory",
                        uriBuilder -> uriBuilder.queryParam("skuCode", skuCodes).build())
                .retrieve()
                .bodyToMono(InventoryResponse[].class)
                .block();
        for(int i = 0;i<inventoryResponses.length; i++){
            System.out.println(inventoryResponses[i].getSkuCode()+" "+inventoryResponses[i].isInStock());
        }
        boolean allProductIsInStock = Arrays.stream(inventoryResponses).allMatch(InventoryResponse::isInStock);
        if(allProductIsInStock)
            orderRepository.save(order);
        else
            throw new IllegalArgumentException("Product is not in stock, please try again later");
    }

    private OrderLineItems mapToDto(OrderLineItemsDto orderLineItemsDto) {
        OrderLineItems orderLineItems = new OrderLineItems();
        orderLineItems.setPrice(orderLineItemsDto.getPrice());
        orderLineItems.setQuantity(orderLineItemsDto.getQuantity());
        orderLineItems.setSkuCode(orderLineItemsDto.getSkuCode());
        return orderLineItems;
    }
}
