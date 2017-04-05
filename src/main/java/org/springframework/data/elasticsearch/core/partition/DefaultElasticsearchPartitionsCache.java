package org.springframework.data.elasticsearch.core.partition;

import org.elasticsearch.action.admin.cluster.state.ClusterStateResponse;
import org.elasticsearch.action.admin.indices.get.GetIndexResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.cluster.metadata.MappingMetaData;
import org.elasticsearch.common.collect.ImmutableOpenMap;
import org.elasticsearch.indices.IndexCreationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Created by franck.lefebure on 25/02/2016.
 */
public class DefaultElasticsearchPartitionsCache implements ElasticsearchPartitionsCache {

    Logger logger = LoggerFactory.getLogger(DefaultElasticsearchPartitionsCache.class);
    ElasticsearchOperations elasticsearchOperations;

    Client client;

    public DefaultElasticsearchPartitionsCache(Client client) {
        this.client = client;
    }

    public void setElasticsearchOperations(ElasticsearchOperations elasticsearchOperations) {
        this.elasticsearchOperations = elasticsearchOperations;
    }

    @Override
    @CacheEvict(value = "esPartitions", allEntries = true)
    public <T> void createPartition(String partition, Class<T> clazz) {
        try {
            logger.info("creating index "+partition);
            elasticsearchOperations.createIndex(clazz, null, partition);
        }
        catch (IndexCreationException exception) {
            // ignore such exception
            logger.info("tried to create existing partition");
        }
    }

    @Override
    @CacheEvict(value = "esPartitions", allEntries = true)
    public <T> void putMapping(String partition, Class<T> clazz) {
        logger.info("creating mapping for class "+clazz.getCanonicalName()+" in index "+partition);
        elasticsearchOperations.putMapping(clazz, null, partition);
    }

    @Override
    @Cacheable(value = "esPartitions")
    public List<String> listTypesForPartition(String partition) {
        List<String> types = new ArrayList<String>();
        ClusterStateResponse clusterStateResponse = client.admin().cluster().prepareState().execute().actionGet();
        ImmutableOpenMap<String,MappingMetaData> indexMappings = clusterStateResponse.getState().getMetaData().index(partition).getMappings();
        Iterator<String> it = indexMappings.keysIt();
        while (it.hasNext()) {
            types.add(it.next());
        }
        logger.trace("listing types for partition "+partition+" "+types);
        return types;
    }


    @Override
    @Cacheable(value = "esPartitions")
    public List<String> listIndicesForPrefix(String prefix) {
        List<String> indices = new ArrayList<String>();
        GetIndexResponse response = client.admin().indices().prepareGetIndex().execute().actionGet();
        for (String index : response.getIndices()) {
            if (index.startsWith(prefix)) {
                logger.debug("adding indice " + index + " to partition cache");
                indices.add(index);
            }
        }
        logger.trace("listing indexes for prefix "+prefix+" "+indices);
        return indices;
    }
}
