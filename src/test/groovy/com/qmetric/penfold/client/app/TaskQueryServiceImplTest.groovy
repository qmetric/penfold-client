package com.qmetric.penfold.client.app

import com.qmetric.penfold.client.app.TaskQueryServiceImpl
import com.sun.jersey.api.client.Client
import com.sun.jersey.api.client.ClientResponse
import com.sun.jersey.api.client.WebResource
import com.sun.jersey.core.util.MultivaluedMapImpl
import com.theoryinpractise.halbuilder.api.RepresentationFactory
import com.qmetric.penfold.client.app.commands.filter.EqualsFilter
import com.qmetric.penfold.client.app.support.Credentials
import com.qmetric.penfold.client.app.support.LocalDateTimeSource
import com.qmetric.penfold.client.app.support.ObjectMapperFactory
import com.qmetric.penfold.client.domain.model.Payload
import com.qmetric.penfold.client.domain.model.QueueId
import com.qmetric.penfold.client.domain.model.Task
import com.qmetric.penfold.client.domain.model.TaskId
import spock.lang.Specification

import javax.ws.rs.core.MultivaluedMap
import java.time.LocalDateTime

import static com.qmetric.penfold.client.domain.model.TaskStatus.READY

class TaskQueryServiceImplTest extends Specification {

    public static final Task expectedTask = new Task(new TaskId("1"), "1", new QueueId("q1"), READY, LocalDateTime.of(2014, 2, 25, 12, 0, 0), 1, new Payload([type: "type1"]))

    final currentDate = LocalDateTime.of(2014, 9, 1, 12, 0, 0, 0)

    final MultivaluedMap<String, String> queryParams = new MultivaluedMapImpl()

    final client = Mock(Client)

    final dateTimeSource = Mock(LocalDateTimeSource)

    final queryRepository = new TaskQueryServiceImpl("http://localhost", credentials(), client, ObjectMapperFactory.create())

    def setup()
    {
        dateTimeSource.now() >> currentDate
    }

    def "should find task by id"()
    {
        given:
        final id = new TaskId("1")
        setupTasksRetrievalResponse("http://localhost/tasks/1", queryParams, "/fixtures/api/task.json")

        when:
        final task = queryRepository.find(id)

        then:
        task == Optional.of(expectedTask.builder().withVersion("2").build())
    }

    def "should query tasks by queue"()
    {
        given:
        setupTasksRetrievalResponse("http://localhost/queues/q1/ready", queryParams, "/fixtures/api/tasks_page.json")

        when:
        final iterator = queryRepository.find(new QueueId("q1"), READY, [])

        then:
        iterator.toList() == [expectedTask]
    }

    def "should query tasks by queue and filters"()
    {
        given:
        final filter = EqualsFilter.of("type", "type1")
        appendQueryParam("""[{"op":"EQ","key":"${filter.key}","value":"${filter.value}"}]""")
        setupTasksRetrievalResponse("http://localhost/queues/q1/ready", queryParams, "/fixtures/api/tasks_page.json")

        when:
        final iterator = queryRepository.find(new QueueId("q1"), READY, [filter])

        then:
        iterator.toList() == [expectedTask]
    }

    def "should retrieve all tasks"()
    {
        given:
        setupTasksRetrievalResponse("http://localhost/tasks", queryParams, "/fixtures/api/tasks_page.json")

        when:
        final iterator = queryRepository.find([])

        then:
        iterator.toList() == [expectedTask]
    }

    def "should query tasks by filters"()
    {
        given:
        final filter = EqualsFilter.of("type", "type1")
        appendQueryParam("""[{"op":"EQ","key":"${filter.key}","value":"${filter.value}"}]""")
        setupTasksRetrievalResponse("http://localhost/tasks", queryParams, "/fixtures/api/tasks_page.json")

        when:
        final iterator = queryRepository.find([filter])

        then:
        iterator.toList() == [expectedTask]
    }

    private def setupTasksRetrievalResponse(final String url, final MultivaluedMap<String, String> queryParams, final String expectedJson)
    {
        final ClientResponse response = expectedResponse(expectedJson)
        final resourceBuilder = Mock(WebResource.Builder)
        final webResource = Mock(WebResource)
        client.resource(url) >> webResource
        webResource.queryParams(queryParams) >> webResource
        webResource.accept(RepresentationFactory.HAL_JSON) >> resourceBuilder
        resourceBuilder.get(ClientResponse.class) >> response
    }

    private ClientResponse expectedResponse(final String jsonPath)
    {
        final response = Mock(ClientResponse)
        response.getEntityInputStream() >> this.getClass().getResource(jsonPath).newInputStream()
        response.getStatus() >> 200
        response
    }

    private void appendQueryParam(final String queryStr)
    {
        queryParams.add("q", queryStr)
    }

    def Credentials credentials()
    {
        new Credentials("user", "pwd")
    }
}
