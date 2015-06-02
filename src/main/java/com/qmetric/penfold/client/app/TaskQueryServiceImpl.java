package com.qmetric.penfold.client.app;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qmetric.hal.reader.HalReader;
import com.qmetric.hal.reader.HalResource;
import com.qmetric.penfold.client.app.commands.filter.Filter;
import com.qmetric.penfold.client.app.support.Credentials;
import com.qmetric.penfold.client.app.support.QuerySerializer;
import com.qmetric.penfold.client.domain.model.PageReference;
import com.qmetric.penfold.client.domain.model.QueueId;
import com.qmetric.penfold.client.domain.model.Task;
import com.qmetric.penfold.client.domain.model.TaskId;
import com.qmetric.penfold.client.domain.model.TaskStatus;
import com.qmetric.penfold.client.domain.model.TasksPage;
import com.qmetric.penfold.client.domain.services.PageAwareTaskQueryService;
import com.qmetric.penfold.client.domain.services.QueueIterator;
import com.qmetric.penfold.client.domain.services.TaskIterator;
import com.qmetric.penfold.client.domain.services.TaskQueryService;
import com.theoryinpractise.halbuilder.api.Link;
import com.theoryinpractise.halbuilder.api.RepresentationFactory;
import org.glassfish.jersey.client.authentication.HttpAuthenticationFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkState;
import static java.lang.String.format;

public class TaskQueryServiceImpl implements TaskQueryService, PageAwareTaskQueryService
{
    private static final Logger LOG = LoggerFactory.getLogger(TaskQueryServiceImpl.class);

    private static final String ACCEPT = RepresentationFactory.HAL_JSON;

    private static final String RETRIEVE_TASKS_BY_QUEUE_URI_TEMPLATE = "%s/queues/%s/%s";

    private static final String RETRIEVE_TASK_URI_TEMPLATE = "%s/tasks/%s";

    private static final String RETRIEVE_TASKS_TEMPLATE = "%s/tasks";

    private static final String TASK_ENTRIES_LINK_REL = "tasks";

    private static final String PREVIOUS_LINK_REL = "previous";

    private static final String NEXT_LINK_REL = "next";

    private final String baseUri;

    private final Client client;

    private final HalReader halReader;

    private final TaskResourceMapper resourceMapper = new TaskResourceMapper();

    private final QuerySerializer querySerializer;

    public TaskQueryServiceImpl(final String baseUri, final Credentials credentials, final Client client, final ObjectMapper objectMapper)
    {
        this.baseUri = baseUri;
        this.client = client;
        this.client.register(HttpAuthenticationFeature.basic(credentials.username, credentials.password));
        this.halReader = new HalReader(objectMapper);
        this.querySerializer = new QuerySerializer(objectMapper);
    }

    @Override public Optional<Task> find(final TaskId id)
    {
        final Optional<HalResource> tasksResource = getTaskResource(id);

        return tasksResource.isPresent() ? Optional.of(resourceMapper.getTaskFromResource(tasksResource.get())) : Optional.empty();
    }

    @Override public Iterator<Task> find(final QueueId queue, final TaskStatus status, final List<Filter> filters)
    {
        return new QueueIterator(queue, status, filters, this);
    }

    @Override public Iterator<Task> find(final List<Filter> filters)
    {
        return new TaskIterator(filters, this);
    }

    @Override public TasksPage retrieve(final QueueId queue, final TaskStatus status, final List<Filter> filters, final Optional<PageReference> pageRequest)
    {
        final String url = format(RETRIEVE_TASKS_BY_QUEUE_URI_TEMPLATE, baseUri, queue.value, status);

        return retrieve(url, filters, pageRequest);
    }

    @Override public TasksPage retrieve(final List<Filter> filters, final Optional<PageReference> pageRequest)
    {
        final String url = format(RETRIEVE_TASKS_TEMPLATE, baseUri);

        return retrieve(url, filters, pageRequest);
    }

    private TasksPage retrieve(final String url, final List<Filter> filters, final Optional<PageReference> pageRequest)
    {
        final MultivaluedMap<String, String> queryString = queryString(filters);
        appendPageParamToRequestIfPresent(pageRequest, queryString);

        final HalResource tasksResource = get(url, queryString);

        final Optional<PageReference> previousPageReference = pageReferenceFrom(tasksResource.getLinkByRel(PREVIOUS_LINK_REL));

        final Optional<PageReference> nextPageReference = pageReferenceFrom(tasksResource.getLinkByRel(NEXT_LINK_REL));

        final List<Task> tasks = getTasksFromPage(tasksResource);

        return new TasksPage(tasks, previousPageReference, nextPageReference);
    }

    private void appendFiltersParamToRequestIfPresent(final MultivaluedMap<String, String> queryParams, final List<Filter> filters)
    {
        final Optional<String> queryValueAsString = querySerializer.serialize(filters);

        if (queryValueAsString.isPresent())
        {
            queryParams.add("q", encode(queryValueAsString));
        }
    }

    private String encode(final Optional<String> queryValueAsString)
    {
        try
        {
            return URLEncoder.encode(queryValueAsString.get(), "UTF-8");
        }
        catch (UnsupportedEncodingException e)
        {
            LOG.error("unable to encode query string param", e);
            throw new RuntimeException(e);
        }
    }

    private void appendPageParamToRequestIfPresent(final Optional<PageReference> pageRequest, final MultivaluedMap<String, String> queryParams)
    {
        if (pageRequest.isPresent())
        {
            queryParams.add("page", pageRequest.get().value);
        }
    }

    private List<Task> getTasksFromPage(final HalResource queueResource)
    {
        final List<HalResource> queueEntries = queueResource.getResourcesByRel(TASK_ENTRIES_LINK_REL);

        return queueEntries.stream() //
                .map(resourceMapper::getTaskFromResource) //
                .collect(Collectors.toList());
    }

    private Optional<PageReference> pageReferenceFrom(final com.google.common.base.Optional<Link> link)
    {
        if (link.isPresent())
        {
            return Optional.of(new PageReference(link.get().getName()));
        }
        else
        {
            return Optional.empty();
        }
    }

    private MultivaluedMap<String, String> queryString(final List<Filter> filters)
    {
        final MultivaluedMap<String, String> queryParams = new MultivaluedHashMap<>();

        appendFiltersParamToRequestIfPresent(queryParams, filters);

        return queryParams;
    }

    private HalResource get(final String url, final MultivaluedMap<String, String> params)
    {
        final WebTarget target = params.entrySet().stream()
                .reduce(client.target(url),
                        (previous, updated) -> previous.queryParam(updated.getKey(), updated.getValue().toArray()),
                        (previous, updated) -> updated);

        final Response response = target.request(ACCEPT).get();

        checkResponseStatus(response, 200);

        return halReader.read(new StringReader(response.readEntity(String.class)));
    }

    private Optional<HalResource> getTaskResource(final TaskId taskId)
    {
        final Response response = client.target(format(RETRIEVE_TASK_URI_TEMPLATE, baseUri, taskId.value)).request(ACCEPT).get();
        if (response.getStatus() == 404)
        {
            return Optional.empty();
        }
        else
        {
            checkResponseStatus(response, 200);

            return Optional.of(halReader.read(new StringReader(response.readEntity(String.class))));
        }
    }

    private void checkResponseStatus(final Response response, final int status)
    {
        checkState(response.getStatus() == status, "unexpected response %s", response.getStatus());
    }
}
