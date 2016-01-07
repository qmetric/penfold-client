package com.qmetric.penfold.client.app;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.qmetric.hal.reader.HalReader;
import com.qmetric.hal.reader.HalResource;
import com.qmetric.penfold.client.app.commands.filter.Filter;
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
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkState;
import static java.lang.String.format;
import static java.net.HttpURLConnection.HTTP_NOT_FOUND;

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

    private final HttpClient client;

    private final HalReader halReader;

    private final TaskResourceMapper resourceMapper = new TaskResourceMapper();

    private final QuerySerializer querySerializer;

    public TaskQueryServiceImpl(final String baseUri, final HttpClient client, final ObjectMapper objectMapper)
    {
        this.baseUri = baseUri;
        this.client = client;
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
        final Multimap<String, String> queryString = queryString(filters);
        appendPageParamToRequestIfPresent(pageRequest, queryString);

        final HalResource tasksResource = getHalResource(url, queryString);

        final Optional<PageReference> previousPageReference = pageReferenceFrom(tasksResource.getLinkByRel(PREVIOUS_LINK_REL));

        final Optional<PageReference> nextPageReference = pageReferenceFrom(tasksResource.getLinkByRel(NEXT_LINK_REL));

        final List<Task> tasks = getTasksFromPage(tasksResource);

        return new TasksPage(tasks, previousPageReference, nextPageReference);
    }

    private void appendFiltersParamToRequestIfPresent(final Multimap<String, String> queryParams, final List<Filter> filters)
    {
        final Optional<String> queryValueAsString = querySerializer.serialize(filters);

        if (queryValueAsString.isPresent())
        {
            queryParams.put("q", queryValueAsString.get());
        }
    }

    private void appendPageParamToRequestIfPresent(final Optional<PageReference> pageRequest, final Multimap<String, String> queryParams)
    {
        if (pageRequest.isPresent())
        {
            queryParams.put("page", pageRequest.get().value);
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

    private Multimap<String, String> queryString(final List<Filter> filters)
    {
        final Multimap<String, String> queryParams = HashMultimap.create();

        appendFiltersParamToRequestIfPresent(queryParams, filters);

        return queryParams;
    }

    private Optional<HalResource> getTaskResource(final TaskId taskId)
    {
        final Optional<Reader> response = get(format(RETRIEVE_TASK_URI_TEMPLATE, baseUri, taskId.value), HashMultimap.create());
        if (!response.isPresent())
        {
            return Optional.empty();
        }
        return Optional.of(halReader.read(response.get()));
    }

    private HalResource getHalResource(final String url, final Multimap<String, String> params)
    {
        final Optional<Reader> response = get(url, params);
        if (!response.isPresent())
        {
            checkResponseStatus(404, 200);
        }
        return halReader.read(response.get());
    }

    private Optional<Reader> get(final String url, final Multimap<String, String> params)
    {
        HttpResponse response = null;
        try
        {
            final List<NameValuePair> nameValuePairs = params.entries().stream()
                    .map((entry) -> new BasicNameValuePair(entry.getKey(), entry.getValue()))
                    .collect(Collectors.toList());

            final URI uri = new URIBuilder(url).addParameters(nameValuePairs).build();

            final HttpGet httpGet = new HttpGet(uri);
            httpGet.addHeader(new BasicHeader(HttpHeaders.ACCEPT, ACCEPT));
            response = client.execute(httpGet);

            final int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode == HTTP_NOT_FOUND)
            {
                return Optional.empty();
            }
            checkResponseStatus(statusCode, 200);

            String entity = EntityUtils.toString(response.getEntity(), Charset.forName("UTF-8"));
            return Optional.<Reader>of(new StringReader(entity));
        }
        catch (IOException e)
        {
            throw new RuntimeException("Error getting HAL feed: ", e);
        }
        catch (URISyntaxException e)
        {
            throw new RuntimeException("Invalid URL for penfold client:", e);
        }
        finally
        {
            if (response != null)
            {
                EntityUtils.consumeQuietly(response.getEntity());
            }
        }
    }

    private void checkResponseStatus(final int actualStatusCode, final int expectedStatusCode)
    {
        checkState(actualStatusCode == expectedStatusCode, "unexpected response %s", actualStatusCode);
    }
}
