package com.sparta.elk.infrastructure.elasticsearch;

import ch.qos.logback.core.testUtil.RandomUtil;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.QueryBuilders;
import com.sparta.elk.application.model.OrderStatus;
import com.sparta.elk.application.model.SearchOrder;
import com.sparta.elk.application.request.OrderCreateRequest;
import com.sparta.elk.application.response.OrderSearchResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.client.elc.NativeQueryBuilder;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
public class SearchOrderServiceImpl {
    private final ElasticsearchOperations elasticsearchOperations;
    private final OrderElasticSearchRepository orderElasticSearchRepository;

    public SearchOrderServiceImpl(ElasticsearchOperations elasticsearchOperations, OrderElasticSearchRepository orderElasticSearchRepository) {
        this.elasticsearchOperations = elasticsearchOperations;
        this.orderElasticSearchRepository = orderElasticSearchRepository;
    }

    public List<OrderSearchResponse> search(String productName, Long startPrice, Long endPrice) {
        log.info("get orders productName: {}, startPrice: {}, endPrice: {}", productName, startPrice, endPrice);
        NativeQueryBuilder query = NativeQuery.builder();
        BoolQuery.Builder boolQuery = QueryBuilders.bool();

        if (startPrice != null && endPrice != null) {
            Query priceQuery = QueryBuilders.range(builder -> builder
                    .number(numberBuilder -> numberBuilder.field("totalPrice")
                            .gte(startPrice.doubleValue())
                            .lte(endPrice.doubleValue())));
            boolQuery.must(priceQuery);
        }

        boolQuery.must(QueryBuilders.queryString(field ->
                    field.fields(List.of("product_list.name")).query("*%s*".formatted(productName))
                ));

        query.withQuery(boolQuery.build()._toQuery());
        SearchHits<SearchOrder> hits = elasticsearchOperations.search(query.build(), SearchOrder.class);
        return hits.map(hit -> toDto(hit.getContent())).toList();
    }

    private static OrderSearchResponse toDto(SearchOrder order) {
        return OrderSearchResponse.builder()
                .orderId(order.getOrderId())
                .totalPrice(order.getTotalPrice())
                .products(order.getProducts().stream()
                        .map(SearchOrderServiceImpl::toDto)
                        .toList()
                )
                .status(order.getStatus())
                .createdAt(order.getCreatedAt())
                .build();
    }

    private static OrderSearchResponse.ProductResponse toDto(SearchOrder.SearchProduct product) {
        return OrderSearchResponse.ProductResponse.builder()
                .name(product.getName())
                .images(product.getImages())
                .price(product.getPrice())
                .quantity(product.getQuantity())
                .build();
    }

    @Transactional
    public OrderSearchResponse create(OrderCreateRequest request) {
        SearchOrder order = requestToSearchOrder(request);
        orderElasticSearchRepository.save(order);
        return toDto(order);
    }

    public SearchOrder requestToSearchOrder(OrderCreateRequest request) {
        return SearchOrder.builder()
                .orderId((long) RandomUtil.getPositiveInt())
                .totalPrice(request.getProducts().stream().mapToLong(OrderCreateRequest.ProductCreateRequest::getPrice).sum())
                .products(request.getProducts().stream()
                        .map(product -> SearchOrder.SearchProduct.builder()
                                .name(product.getName())
                                .price(product.getPrice())
                                .images(product.getImages())
                                .quantity(product.getQuantity())
                                .build()).toList())
                .status(OrderStatus.LOADING_PRODUCT)
                .build();
    }
}
