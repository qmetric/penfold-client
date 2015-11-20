package com.qmetric.penfold.client.app;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Charsets;
import com.qmetric.hal.reader.HalReader;
import com.qmetric.hal.reader.HalResource;
import com.qmetric.penfold.client.app.commands.CancelTaskCommand;
import com.qmetric.penfold.client.app.commands.CloseTaskCommand;
import com.qmetric.penfold.client.app.commands.CreateTaskCommand;
import com.qmetric.penfold.client.app.commands.RequeueTaskCommand;
import com.qmetric.penfold.client.app.commands.RescheduleTaskCommand;
import com.qmetric.penfold.client.app.commands.StartTaskCommand;
import com.qmetric.penfold.client.domain.exceptions.ConflictException;
import com.qmetric.penfold.client.domain.model.CloseResultType;
import com.qmetric.penfold.client.domain.model.CommandType;
import com.qmetric.penfold.client.domain.model.NewTask;
import com.qmetric.penfold.client.domain.model.Task;
import com.qmetric.penfold.client.domain.model.TaskId;
import com.qmetric.penfold.client.domain.services.TaskStoreService;
import com.theoryinpractise.halbuilder.api.Link;
import com.theoryinpractise.halbuilder.api.RepresentationFactory;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicHeader;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.StringReader;
import java.time.LocalDateTime;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkState;
import static java.lang.String.format;

public class TaskStoreServiceImpl implements TaskStoreService
{
    private static final Logger LOG = LoggerFactory.getLogger(TaskStoreServiceImpl.class);

    private static final String ACCEPT = RepresentationFactory.HAL_JSON;

    private static final String CREATE_TASK_URI_TEMPLATE = "%s/tasks";

    private static final String RETRIEVE_TASK_URI_TEMPLATE = "%s/tasks/%s";

    private static final String CONTENT_TYPE_TEMPLATE = "application/json;domain-command=%s";

    private final String baseUri;

    private final HttpClient httpClient;

    private final ObjectMapper objectMapper;

    private final HalReader halReader;

    private final TaskResourceMapper resourceMapper = new TaskResourceMapper();

    public TaskStoreServiceImpl(final String baseUri, final HttpClient httpClient, final ObjectMapper objectMapper)
    {
        this.baseUri = baseUri;
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
        this.halReader = new HalReader(objectMapper);
    }

    @Override public Task create(final NewTask task)
    {
        final String taskJson = toJson(new CreateTaskCommand(task));

        final CommandType commandType = task.triggerDate.isPresent() ? CommandType.CreateFutureTask : CommandType.CreateTask;

        HttpPost httpPost = new HttpPost(format(CREATE_TASK_URI_TEMPLATE, baseUri));
        httpPost.addHeader(HttpHeaders.ACCEPT, ACCEPT);
        httpPost.addHeader(HttpHeaders.CONTENT_TYPE, contentTypeHeaderFor(commandType));
        final StringEntity requestEntity = new StringEntity(taskJson, Charsets.UTF_8);
        httpPost.setEntity(requestEntity);

        HttpResponse response = null;
        try
        {
            response = httpClient.execute(httpPost);
            checkResponseStatus(response.getStatusLine().getStatusCode(), 201);
            return taskFromResponse(response);
        }
        catch (IOException e)
        {
            throw new RuntimeException("Error getting HAL feed: ", e);
        }
        finally
        {
            if (response != null)
            {
                EntityUtils.consumeQuietly(response.getEntity());
            }
        }
    }

    @Override public Task start(final Task task) throws ConflictException
    {
        return update(task, new StartTaskCommand(), CommandType.StartTask);
    }

    @Override public Task requeue(final Task task, final Optional<String> reason) throws ConflictException
    {
        return update(task, new RequeueTaskCommand(reason), CommandType.RequeueTask);
    }

    @Override public Task reschedule(final Task task, final LocalDateTime triggerDate, final Optional<String> reason) throws ConflictException
    {
        return update(task, new RescheduleTaskCommand(triggerDate, reason), CommandType.RescheduleTask);
    }

    @Override public Task cancel(final Task task, final Optional<String> reason) throws ConflictException
    {
        return update(task, new CancelTaskCommand(reason), CommandType.CancelTask);
    }

    @Override public Task close(final Task task, final Optional<CloseResultType> resultType, final Optional<String> reason) throws ConflictException
    {
        return update(task, new CloseTaskCommand(resultType, reason), CommandType.CloseTask);
    }

    private Task update(final Task task, final Object command, final CommandType commandType) throws ConflictException
    {
        final HalResource taskResource = getTaskResourceWithExpectedVersion(task.id, task.version);

        final com.google.common.base.Optional<Link> updateTaskLink = taskResource.getLinkByRel(commandType.name());

        if (updateTaskLink.isPresent())
        {
            final String json = toJson(command);

            final HttpPost httpPost = new HttpPost(updateTaskLink.get().getHref());
            httpPost.addHeader(HttpHeaders.ACCEPT, ACCEPT);
            httpPost.addHeader(HttpHeaders.CONTENT_TYPE, contentTypeHeaderFor(commandType));
            httpPost.setEntity(new StringEntity(format(json, task.id), Charsets.UTF_8));

            HttpResponse response = null;
            try
            {
                response = httpClient.execute(httpPost);
                final int responseStatus = response.getStatusLine().getStatusCode();
                if (responseStatus == 409)
                {
                    throw new ConflictException(String.format("conflict when attempting to %s task %s", commandType, task.id));
                }
                checkResponseStatus(response.getStatusLine().getStatusCode(), 200);
                return taskFromResponse(response);
            }
            catch (IOException e)
            {
                throw new RuntimeException("Error getting HAL feed: ", e);
            }
            finally
            {
                if (response != null)
                {
                    EntityUtils.consumeQuietly(response.getEntity());
                }
            }
        }
        else
        {
            throw new ConflictException(String.format("conflict when attempting to %s %s", commandType, task.id));
        }
    }

    private HalResource getTaskResource(final TaskId taskId)
    {
        final HttpGet httpGet = new HttpGet(format(RETRIEVE_TASK_URI_TEMPLATE, baseUri, taskId));
        httpGet.addHeader(new BasicHeader(HttpHeaders.ACCEPT, ACCEPT));

        HttpResponse response = null;
        try
        {
            response = httpClient.execute(httpGet);

            final int statusCode = response.getStatusLine().getStatusCode();
            checkResponseStatus(statusCode, 200);
            String entity = EntityUtils.toString(response.getEntity(), Charsets.UTF_8);
            return halReader.read(new StringReader(entity));
        }
        catch (IOException e)
        {
            throw new RuntimeException("Error getting task with id: " + taskId, e);
        }
        finally
        {
            if (response != null)
            {
                EntityUtils.consumeQuietly(response.getEntity());
            }
        }
    }

    private HalResource getTaskResourceWithExpectedVersion(final TaskId id, final Integer expectedVersion)
    {
        final HalResource halResource = getTaskResource(id);

        if (expectedVersion.equals(Integer.valueOf(halResource.getValueAsString("version").orNull())))
        {
            return halResource;
        }
        else
        {
            throw new ConflictException(String.format("Task merge conflict %s", id));
        }
    }

    private void checkResponseStatus(final int actualStatusCode, final int expectedStatusCode)
    {
        checkState(actualStatusCode == expectedStatusCode, "Unexpected response %s", actualStatusCode);
    }

    private String toJson(final Object object)
    {
        try
        {
            return objectMapper.writeValueAsString(object);
        }
        catch (final JsonProcessingException e)
        {
            LOG.error(String.format("failed to parse command %s", object), e);
            throw new RuntimeException(e);
        }
    }

    private Task taskFromResponse(final HttpResponse response) throws IOException
    {
        final String reponseString = EntityUtils.toString(response.getEntity());
        final HalResource taskHalResource = halReader.read(new StringReader(reponseString));

        return resourceMapper.getTaskFromResource(taskHalResource);
    }

    private String contentTypeHeaderFor(final CommandType commandType)
    {
        return String.format(CONTENT_TYPE_TEMPLATE, commandType.name());
    }
}
