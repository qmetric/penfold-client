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
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.filter.HTTPBasicAuthFilter;
import com.sun.jersey.core.util.MultivaluedMapImpl;
import com.theoryinpractise.halbuilder.api.Link;
import com.theoryinpractise.halbuilder.api.RepresentationFactory;

import javax.ws.rs.core.MultivaluedMap;

import java.io.InputStreamReader;
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
        this.client.addFilter(new HTTPBasicAuthFilter(credentials.username, credentials.password));
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
        final MultivaluedMap<String, String> queryParams = new MultivaluedMapImpl();

        appendFiltersParamToRequestIfPresent(queryParams, filters);

        return queryParams;
    }

    private HalResource get(final String url, final MultivaluedMap<String, String> params)
    {
        final ClientResponse response = client.resource(url) //
                .queryParams(params).accept(ACCEPT) //
                .get(ClientResponse.class);

        checkResponseStatus(response, 200);

        return halReader.read(new InputStreamReader(response.getEntityInputStream()));
    }

    private Optional<HalResource> getTaskResource(final TaskId taskId)
    {
        final ClientResponse response = client.resource(format(RETRIEVE_TASK_URI_TEMPLATE, baseUri, taskId.value)).accept(ACCEPT).get(ClientResponse.class);
        if (response.getStatus() == 404)
        {
            return Optional.empty();
        }
        else
        {
            checkResponseStatus(response, 200);

            return Optional.of(halReader.read(new InputStreamReader(response.getEntityInputStream())));
        }
    }

    private void checkResponseStatus(final ClientResponse response, final int status)
    {
        checkState(response.getStatus() == status, "unexpected response %s", response.getStatus());
    }
}
