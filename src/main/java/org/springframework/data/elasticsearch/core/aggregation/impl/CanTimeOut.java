package org.springframework.data.elasticsearch.core.aggregation.impl;

/**
 * Created by smazet on 20/03/18.
 */
public interface CanTimeOut {

    boolean isTimedOut();

    void setTimedOut(boolean timedOut);

}
