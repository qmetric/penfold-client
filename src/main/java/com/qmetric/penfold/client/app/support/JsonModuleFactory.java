package com.qmetric.penfold.client.app.support;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.qmetric.penfold.client.domain.model.QueueId;
import com.qmetric.penfold.client.domain.model.TaskId;

import java.io.IOException;
import java.time.LocalDateTime;

import static com.fasterxml.jackson.core.Version.unknownVersion;

public class JsonModuleFactory
{
    public static Module create()
    {
        final SimpleModule module = new SimpleModule("jacksonConfig", unknownVersion());

        module.addSerializer(TaskId.class, new TaskIdJsonSerializer());
        module.addSerializer(QueueId.class, new QueueIdJsonSerializer());
        module.addSerializer(LocalDateTime.class, new LocalDateTimeJsonSerializer());

        return module;
    }

    private static class TaskIdJsonSerializer extends JsonSerializer<TaskId>
    {
        @Override public void serialize(final TaskId taskId, final JsonGenerator jsonGenerator, final SerializerProvider serializerProvider) throws IOException
        {
            jsonGenerator.writeString(taskId.toString());
        }
    }

    private static class QueueIdJsonSerializer extends JsonSerializer<QueueId>
    {
        @Override public void serialize(final QueueId queueId, final JsonGenerator jsonGenerator, final SerializerProvider serializerProvider) throws IOException
        {
            jsonGenerator.writeString(queueId.value);
        }
    }

    private static class LocalDateTimeJsonSerializer extends JsonSerializer<LocalDateTime>
    {
        @Override public void serialize(final LocalDateTime dateTime, final JsonGenerator jsonGenerator, final SerializerProvider serializerProvider) throws IOException
        {
            jsonGenerator.writeString(TaskDateTimeFormatter.print(dateTime));
        }
    }
}
