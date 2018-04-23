package org.springframework.data.elasticsearch.core;

import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.common.unit.TimeValue;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.core.aggregation.AggregatedPage;
import org.springframework.data.elasticsearch.core.mapping.ElasticsearchPersistentEntity;
import org.springframework.data.elasticsearch.core.mapping.ElasticsearchPersistentProperty;
import org.springframework.data.mapping.context.MappingContext;

/**
 * Created by smazet on 23/03/18.
 */
class CumulativeTimeOutMapper extends DefaultResultMapper {

    private final TimeValue timeOut;
    private volatile long totalTime;

    public CumulativeTimeOutMapper(TimeValue timeOut) {
        super();
        this.timeOut = timeOut;
        this.totalTime = 0;
    }

    public CumulativeTimeOutMapper(MappingContext<? extends ElasticsearchPersistentEntity<?>, ElasticsearchPersistentProperty> mappingContext, TimeValue timeOut) {
        super(mappingContext);
        this.timeOut = timeOut;
    }

    @Override
    public <T> AggregatedPage<T> mapResults(SearchResponse response, Class<T> clazz, Pageable pageable) {
        AggregatedPage<T> page = super.mapResults(response, clazz, pageable);
        this.totalTime += response.getTook().getMillis();
        if (this.timeOut != TimeValue.MINUS_ONE && this.timeOut!=TimeValue.ZERO
             && this.totalTime > timeOut.millis()) {
            page.setTimedOut(true);
        }
        return page;
    }
}
