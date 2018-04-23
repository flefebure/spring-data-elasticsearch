package org.springframework.data.elasticsearch.core;

import org.elasticsearch.ElasticsearchTimeoutException;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.support.PlainListenableActionFuture;
import org.elasticsearch.threadpool.ThreadPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by smazet on 22/03/18.
 */
class TimeOutListenableActionFuture<Response extends SearchResponse> extends PlainListenableActionFuture<Response> {

    private static Logger logger = LoggerFactory.getLogger(TimeOutListenableActionFuture.class);
    private final long timeOutMillis;
    private final String name;

    public TimeOutListenableActionFuture(ThreadPool threadPool, long timeOutMillis, String name) {
        super();
        this.timeOutMillis = timeOutMillis;
        this.name = name;
    }

    @Override
    protected Response convert(Response response) {
        if (response.isTimedOut() || (this.timeOutMillis!=0 && response.getTook().getMillis()>this.timeOutMillis)) {
            throw new ElasticsearchTimeoutException("Time out, query took " + response.getTook().getMillis()+" millis");
        }
        return response;
    }

}
