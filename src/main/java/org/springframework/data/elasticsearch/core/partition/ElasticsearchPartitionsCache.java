package org.springframework.data.elasticsearch.core.partition;

import java.util.List;

/**
 * Created by franck.lefebure on 25/02/2016.
 */
public interface ElasticsearchPartitionsCache {

    public List<String> listIndicesForPrefix(String prefix);
    public List<String> listTypesForPartition(String partition);
    public <T> void createPartition(String partition, Class<T> clazz);
    public <T> void putMapping(String partition, Class<T> clazz);
}
