package com.sparta.elk.presentation;

import com.sparta.elk.application.request.OrderCreateRequest;
import com.sparta.elk.application.response.OrderSearchResponse;
import com.sparta.elk.infrastructure.elasticsearch.SearchOrderServiceImpl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
public class OrderController {
    private final SearchOrderServiceImpl orderService;

    public OrderController(SearchOrderServiceImpl orderService) {
        this.orderService = orderService;
    }

    @GetMapping
    public ResponseEntity<List<OrderSearchResponse>> getOrders(
            @RequestParam(value = "product") String productName,
            @RequestParam(value = "start_price", required = false) Long startPrice,
            @RequestParam(value = "end_price", required = false) Long endPrice
    ) {
        List<OrderSearchResponse> response = orderService.search(productName, startPrice, endPrice);
        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<OrderSearchResponse> createOrder(
            @RequestBody OrderCreateRequest request
    ) {
        OrderSearchResponse response = orderService.create(request);
        return ResponseEntity.ok(response);
    }
}
