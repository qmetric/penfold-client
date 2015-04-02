package com.qmetric.penfold.client.app.support;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qmetric.penfold.client.app.commands.filter.Filter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import static org.apache.commons.collections.CollectionUtils.isNotEmpty;

public class QuerySerializer
{
    private static final Logger LOGGER = LoggerFactory.getLogger(QuerySerializer.class);

    private final ObjectMapper objectMapper;

    public QuerySerializer(final ObjectMapper objectMapper)
    {
        this.objectMapper = objectMapper;
    }

    public Optional<String> serialize(final List<Filter> filters)
    {
        try
        {
            if (isNotEmpty(filters))
            {
                return Optional.of(objectMapper.writeValueAsString(filters));
            }
            else
            {
                return Optional.empty();
            }
        }
        catch (IOException e)
        {
            LOGGER.error("invalid search filters in request", e);
            throw new IllegalArgumentException("invalid search filters");
        }
    }
}
