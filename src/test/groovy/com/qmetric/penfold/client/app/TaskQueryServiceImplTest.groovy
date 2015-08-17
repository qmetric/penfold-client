package com.qmetric.penfold.client.app

import com.qmetric.penfold.client.app.commands.filter.EqualsFilter
import com.qmetric.penfold.client.app.support.Credentials
import com.qmetric.penfold.client.app.support.LocalDateTimeSource
import com.qmetric.penfold.client.app.support.ObjectMapperFactory
import com.qmetric.penfold.client.domain.model.Payload
import com.qmetric.penfold.client.domain.model.QueueId
import com.qmetric.penfold.client.domain.model.Task
import com.qmetric.penfold.client.domain.model.TaskId
import com.theoryinpractise.halbuilder.api.RepresentationFactory
import spock.lang.Specification

import javax.ws.rs.client.Client
import javax.ws.rs.client.Invocation
import javax.ws.rs.client.WebTarget
import javax.ws.rs.core.MultivaluedHashMap
import javax.ws.rs.core.MultivaluedMap
import javax.ws.rs.core.Response
import java.time.LocalDateTime

import static com.qmetric.penfold.client.domain.model.TaskStatus.READY

class TaskQueryServiceImplTest extends Specification {

    public static final LocalDateTime created = LocalDateTime.of(2014, 2, 25, 12, 0, 0)

    public static final LocalDateTime triggerDate = LocalDateTime.of(2014, 04, 15, 10, 35, 5, 0)

    public static final Task expectedTask = new Task(new TaskId("1"), 1, new QueueId("q1"), READY, created, triggerDate, 1, new Payload([type: "type1"]))

    final currentDate = LocalDateTime.of(2014, 9, 1, 12, 0, 0, 0)

    final MultivaluedMap<String, String> queryParams = new MultivaluedHashMap<>()

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
        task == Optional.of(expectedTask.builder().withVersion(2).build())
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
        appendQueryParam("%5B%7B%22op%22%3A%22EQ%22%2C%22key%22%3A%22${filter.key}%22%2C%22value%22%3A%22${filter.value}%22%7D%5D")
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
        appendQueryParam("%5B%7B%22op%22%3A%22EQ%22%2C%22key%22%3A%22${filter.key}%22%2C%22value%22%3A%22${filter.value}%22%7D%5D")
        setupTasksRetrievalResponse("http://localhost/tasks", queryParams, "/fixtures/api/tasks_page.json")

        when:
        final iterator = queryRepository.find([filter])

        then:
        iterator.toList() == [expectedTask]
    }

    private def setupTasksRetrievalResponse(final String url, final MultivaluedMap<String, String> queryParams, final String expectedJson)
    {
        final Response response = expectedResponse(expectedJson)
        final builder = Mock(Invocation.Builder)
        final webTarget = Mock(WebTarget)
        final webTargetWithQueryParams = Mock(WebTarget)
        client.target(url) >> webTarget
        queryParams.entrySet().each {e -> webTarget.queryParam(e.key, e.value.toArray()) >> webTargetWithQueryParams}
        (queryParams.isEmpty() ? webTarget: webTargetWithQueryParams).request(RepresentationFactory.HAL_JSON) >> builder
        builder.get() >> response
    }

    private Response expectedResponse(final String jsonPath)
    {
        final response = Mock(Response)
        response.readEntity(String.class) >> this.getClass().getResource(jsonPath).text
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
