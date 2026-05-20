package com.sparta.elk.infrastructure.elasticsearch;

import com.sparta.elk.application.model.SearchOrder;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface OrderElasticSearchRepository extends ElasticsearchRepository<SearchOrder, String> {
}
