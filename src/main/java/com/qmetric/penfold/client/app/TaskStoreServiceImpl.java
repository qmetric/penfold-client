package com.qmetric.penfold.client.app;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qmetric.hal.reader.HalReader;
import com.qmetric.hal.reader.HalResource;
import com.qmetric.penfold.client.app.commands.CancelTaskCommand;
import com.qmetric.penfold.client.app.commands.RequeueTaskCommand;
import com.qmetric.penfold.client.domain.model.CloseResultType;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.filter.HTTPBasicAuthFilter;
import com.theoryinpractise.halbuilder.api.Link;
import com.theoryinpractise.halbuilder.api.RepresentationFactory;
import com.qmetric.penfold.client.domain.services.TaskStoreService;
import com.qmetric.penfold.client.app.support.Credentials;
import com.qmetric.penfold.client.app.commands.CloseTaskCommand;
import com.qmetric.penfold.client.app.commands.CreateTaskCommand;
import com.qmetric.penfold.client.app.commands.RescheduleTaskCommand;
import com.qmetric.penfold.client.app.commands.StartTaskCommand;
import com.qmetric.penfold.client.domain.exceptions.ConflictException;
import com.qmetric.penfold.client.domain.model.CommandType;
import com.qmetric.penfold.client.domain.model.NewTask;
import com.qmetric.penfold.client.domain.model.Task;
import com.qmetric.penfold.client.domain.model.TaskId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStreamReader;
import java.time.LocalDateTime;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkState;
import static java.lang.String.format;

public class TaskStoreServiceImpl implements TaskStoreService
{
    private static final Logger LOGGER = LoggerFactory.getLogger(TaskStoreServiceImpl.class);

    private static final String ACCEPT = RepresentationFactory.HAL_JSON;

    private static final String CREATE_TASK_URI_TEMPLATE = "%s/tasks";

    private static final String RETRIEVE_TASK_URI_TEMPLATE = "%s/tasks/%s";

    private static final String CONTENT_TYPE_TEMPLATE = "application/json;domain-command=%s";

    private final String baseUri;

    private final Client client;

    private final ObjectMapper objectMapper;

    private final HalReader halReader;

    private final TaskResourceMapper resourceMapper = new TaskResourceMapper();

    public TaskStoreServiceImpl(final String baseUri, final Credentials credentials, final Client client, final ObjectMapper objectMapper)
    {
        this.baseUri = baseUri;
        this.client = client;
        this.objectMapper = objectMapper;
        this.client.addFilter(new HTTPBasicAuthFilter(credentials.username, credentials.password));
        this.halReader = new HalReader(objectMapper);
    }

    @Override public Task create(final NewTask task)
    {
        final String taskJson = toJson(new CreateTaskCommand(task));

        final CommandType commandType = task.triggerDate.isPresent() ? CommandType.CreateFutureTask : CommandType.CreateTask;

        final WebResource.Builder webResource = client.resource(format(CREATE_TASK_URI_TEMPLATE, baseUri)).accept(ACCEPT).type(contentTypeHeaderFor(commandType));

        final ClientResponse response = webResource.post(ClientResponse.class, taskJson);
        checkResponseStatus(response, 201);

        return taskFromResponse(response);
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
            final WebResource.Builder webResource = client.resource(updateTaskLink.get().getHref()).accept(ACCEPT).type(contentTypeHeaderFor(commandType));
            final ClientResponse response = webResource.post(ClientResponse.class, format(json, task.id));
            final int responseStatus = response.getStatus();

            if (responseStatus == 409)
            {
                throw new ConflictException(String.format("conflict when attempting to %s task %s", commandType, task.id));
            }

            checkState(responseStatus == 200, "error %s when attempting to %s task %s", responseStatus, commandType, task.id);

            return taskFromResponse(response);
        }
        else
        {
            throw new ConflictException(String.format("conflict when attempting to %s %s", commandType, task.id));
        }
    }

    private HalResource getTaskResource(final TaskId taskId)
    {
        final ClientResponse response = client.resource(format(RETRIEVE_TASK_URI_TEMPLATE, baseUri, taskId)).accept(ACCEPT).get(ClientResponse.class);
        checkResponseStatus(response, 200);

        return halReader.read(new InputStreamReader(response.getEntityInputStream()));
    }

    private HalResource getTaskResourceWithExpectedVersion(final TaskId id, final String expectedVersion)
    {
        final HalResource halResource = getTaskResource(id);

        if (expectedVersion.equals(halResource.getValueAsString("version").orNull()))
        {
            return halResource;
        }
        else
        {
            throw new ConflictException(String.format("Task merge conflict %s", id));
        }
    }

    private void checkResponseStatus(final ClientResponse response, final int status)
    {
        checkState(response.getStatus() == status, "Unexpected response %s", response.getStatus());
    }

    private String toJson(final Object object)
    {
        try
        {
            return objectMapper.writeValueAsString(object);
        }
        catch (final JsonProcessingException e)
        {
            LOGGER.error(String.format("failed to parse command %s", object), e);
            throw new RuntimeException(e);
        }
    }

    private Task taskFromResponse(final ClientResponse response)
    {
        final HalResource taskHalResource = halReader.read(new InputStreamReader(response.getEntityInputStream()));

        return resourceMapper.getTaskFromResource(taskHalResource);
    }

    private String contentTypeHeaderFor(final CommandType commandType)
    {
        return String.format(CONTENT_TYPE_TEMPLATE, commandType.name());
    }
}
