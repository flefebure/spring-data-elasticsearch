package org.springframework.data.elasticsearch.annotations;

import org.springframework.data.elasticsearch.core.partition.keys.DatePartition;
import org.springframework.data.elasticsearch.core.partition.keys.LongPartition;
import org.springframework.data.elasticsearch.core.partition.keys.StringPartition;

/**
 * Created by franck.lefebure on 24/02/2016.
 */
public enum Partitioner {

    fixed_string(StringPartition.class),
    long_range(LongPartition.class),
    date_range(DatePartition.class);

    Class implementation;

    public Class getImplementation() {
        return implementation;
    }

    Partitioner(Class implementation) {
        this.implementation = implementation;
    }
}
