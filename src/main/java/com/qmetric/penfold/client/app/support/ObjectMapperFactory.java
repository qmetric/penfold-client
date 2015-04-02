package com.qmetric.penfold.client.app.support;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;

public class ObjectMapperFactory
{
    public static ObjectMapper create()
    {
        final ObjectMapper objectMapper = new ObjectMapper();

        objectMapper.registerModule(JsonModuleFactory.create());
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
        objectMapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        objectMapper.registerModule(new Jdk8Module());

        return objectMapper;
    }
}
