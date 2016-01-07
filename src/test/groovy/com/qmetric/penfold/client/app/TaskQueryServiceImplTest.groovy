package com.qmetric.penfold.client.app

import com.qmetric.penfold.client.app.commands.filter.EqualsFilter
import com.qmetric.penfold.client.app.support.LocalDateTimeSource
import com.qmetric.penfold.client.app.support.ObjectMapperFactory
import com.qmetric.penfold.client.domain.model.Payload
import com.qmetric.penfold.client.domain.model.QueueId
import com.qmetric.penfold.client.domain.model.Task
import com.qmetric.penfold.client.domain.model.TaskId
import org.apache.http.HttpResponse
import org.apache.http.ProtocolVersion
import org.apache.http.StatusLine
import org.apache.http.client.HttpClient
import org.apache.http.client.methods.HttpUriRequest
import org.apache.http.entity.StringEntity
import org.apache.http.message.BasicStatusLine
import spock.lang.Specification

import java.time.LocalDateTime

import static com.qmetric.penfold.client.domain.model.TaskStatus.READY

class TaskQueryServiceImplTest extends Specification {

    public static final LocalDateTime created = LocalDateTime.of(2014, 2, 25, 12, 0, 0)

    public static final LocalDateTime triggerDate = LocalDateTime.of(2014, 04, 15, 10, 35, 5, 0)

    public static final Task expectedTask = new Task(new TaskId("1"), 1, new QueueId("q1"), READY, created, triggerDate, 1, new Payload([type: "type1"]))

    final currentDate = LocalDateTime.of(2014, 9, 1, 12, 0, 0, 0)

    final client = Mock(HttpClient)

    final dateTimeSource = Mock(LocalDateTimeSource)

    final queryRepository = new TaskQueryServiceImpl("http://localhost", client, ObjectMapperFactory.create())

    def setup()
    {
        dateTimeSource.now() >> currentDate
    }

    def "should find task by id"()
    {
        given:
        final id = new TaskId("1")
        setupTasksRetrievalResponse("http://localhost/tasks/1", "/fixtures/api/task.json")

        when:
        final task = queryRepository.find(id)

        then:
        task == Optional.of(expectedTask.builder().withVersion(2).build())
    }

    def "should query tasks by queue"()
    {
        given:
        setupTasksRetrievalResponse("http://localhost/queues/q1/ready", "/fixtures/api/tasks_page.json")

        when:
        final iterator = queryRepository.find(new QueueId("q1"), READY, [])

        then:
        iterator.toList() == [expectedTask]
    }

    def "should query tasks by queue and filters"()
    {
        given:
        final filter = EqualsFilter.of("type", "type1")
        setupTasksRetrievalResponse("http://localhost/queues/q1/ready", "/fixtures/api/tasks_page.json", "%5B%7B%22op%22%3A%22EQ%22%2C%22key%22%3A%22${filter.key}%22%2C%22value%22%3A%22${filter.value}%22%7D%5D")

        when:
        final iterator = queryRepository.find(new QueueId("q1"), READY, [filter])

        then:
        iterator.toList() == [expectedTask]
    }

    def "should retrieve all tasks"()
    {
        given:
        setupTasksRetrievalResponse("http://localhost/tasks", "/fixtures/api/tasks_page.json")

        when:
        final iterator = queryRepository.find([])

        then:
        iterator.toList() == [expectedTask]
    }

    def "should query tasks by filters"()
    {
        given:
        final filter = EqualsFilter.of("type", "type1")
        setupTasksRetrievalResponse("http://localhost/tasks", "/fixtures/api/tasks_page.json", "%5B%7B%22op%22%3A%22EQ%22%2C%22key%22%3A%22${filter.key}%22%2C%22value%22%3A%22${filter.value}%22%7D%5D")

        when:
        final iterator = queryRepository.find([filter])

        then:
        iterator.toList() == [expectedTask]
    }

    private def setupTasksRetrievalResponse(final String expectedUrl, final String responseJsonResource, final String expectedQueryParam = "")
    {
        StringEntity entity = new StringEntity(getResource(responseJsonResource))
        StatusLine statusLine = new BasicStatusLine(new ProtocolVersion("HTTP", 1, 1), 200, "OK")
        HttpResponse mockResponse = Mock(HttpResponse)
        mockResponse.getEntity() >> entity
        mockResponse.getStatusLine() >> statusLine
        def expectedUrlWithParams = expectedUrl + "?" + (expectedQueryParam ? "q=" + expectedQueryParam : "")

        client.execute({ request -> request.getURI().toString() == expectedUrlWithParams } as HttpUriRequest) >> mockResponse
    }

    private static String getResource(String name)
    {
        return this.getClass().getResource(name).text
    }

}
