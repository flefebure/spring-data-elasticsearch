
package org.springframework.data.elasticsearch.core;

import org.springframework.data.domain.Page;
import org.springframework.data.elasticsearch.core.aggregation.impl.CanTimeOut;

/**
 * @author Artur Konczak
 */
public interface ScrolledPage<T> extends Page<T>,CanTimeOut {

    String getScrollId();

}
