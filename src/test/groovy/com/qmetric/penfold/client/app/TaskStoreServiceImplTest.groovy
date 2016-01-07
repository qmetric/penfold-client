package com.qmetric.penfold.client.app

import com.google.common.base.Charsets
import com.google.common.io.CharStreams
import com.google.common.io.Closeables
import com.qmetric.penfold.client.app.support.ObjectMapperFactory
import com.qmetric.penfold.client.domain.model.*
import groovy.json.JsonSlurper
import org.apache.http.HttpEntity
import org.apache.http.HttpResponse
import org.apache.http.ProtocolVersion
import org.apache.http.StatusLine
import org.apache.http.client.HttpClient
import org.apache.http.client.methods.HttpUriRequest
import org.apache.http.entity.StringEntity
import org.apache.http.message.BasicStatusLine
import spock.lang.Specification

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

import static java.util.Optional.empty

class TaskStoreServiceImplTest extends Specification {

    static final taskId = new TaskId("1")

    static final queueId = new QueueId("q1")

    static final created = LocalDateTime.of(2014, 3, 15, 10, 35, 5)

    static final attempts = 0

    static final payload = new Payload([type: "type1"])

    static final triggerDate = LocalDateTime.of(2015, 4, 15, 10, 35, 5)

    final client = Mock(HttpClient)

    final store = new TaskStoreServiceImpl("http://localhost", client, ObjectMapperFactory.create())

    def "should create task"()
    {
        given:
        def expectedRequest = getResource("/fixtures/api/command/create_task_command.json")
        def response = getResource("/fixtures/api/create_task_response.json")
        setupRequestBuilder(expectedRequest, response, 201)

        when:
        def task = store.create(new NewTask(queueId, payload, empty()))

        then:
        task.id == new TaskId("1")
        task.version == 2
        task.queue == new QueueId("aggregator")
        task.payload.getAsMap().type == "type1"
        task != null
    }

    def "should create future task"()
    {
        given:
        def expectedRequest = getResource("/fixtures/api/command/create_future_task_command.json")
        def response = getResource("/fixtures/api/create_future_task_response.json")
        setupRequestBuilder(expectedRequest, response, 201)

        when:
        def task = store.create(new NewTask(queueId, payload, Optional.of(triggerDate)))

        then:
        task.id == new TaskId("1")
        task.version == 2
        task.queue == new QueueId("aggregator")
        task.payload.getAsMap().type == "type1"
        task.triggerDate == LocalDateTime.parse("2015-04-15T10:35:05", DateTimeFormatter.ISO_LOCAL_DATE_TIME)
    }

    def "should start task"()
    {
        given:
        def response = getResource("/fixtures/api/task.json")
        setupTaskRetrievalResponse("http://localhost/tasks/1", response)

        def expectedRequest = getResource("/fixtures/api/command/start_task_command.json")
        def postResponse = getResource("/fixtures/api/start_task_response.json")

        setupTaskCommand("http://localhost/tasks/1/2", expectedRequest, postResponse)

        when:
        def start = store.start(createTask())

        then:
        start.id == new TaskId("1")
        start.version == 2
        start.queue == new QueueId("aggregator")
        start.triggerDate == LocalDateTime.parse("2015-04-15T10:35:05", DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        start.status == TaskStatus.STARTED
    }

    def "should requeue task"()
    {
        given:
        def response = getResource("/fixtures/api/task.json")
        setupTaskRetrievalResponse("http://localhost/tasks/1", response)

        def expectedRequest = getResource("/fixtures/api/command/requeue_task_command.json")
        def postResponse = getResource("/fixtures/api/requeue_task_response.json")
        setupTaskCommand("http://localhost/tasks/1/2", expectedRequest, postResponse)

        when:
        def requeue = store.requeue(createTask(), Optional.of("reason1"))

        then:
        requeue != null
        requeue.id == new TaskId("1")
        requeue.version == 2
        requeue.queue == new QueueId("aggregator")
        requeue.triggerDate == LocalDateTime.parse("2015-04-15T10:35:05", DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        requeue.status == TaskStatus.READY
        requeue.payload.getAsMap() == [type:"type1"]
    }

    def "should reschedule task"()
    {
        given:
        def response = getResource("/fixtures/api/task.json")
        setupTaskRetrievalResponse("http://localhost/tasks/1", response)

        def expectedRequest = getResource("/fixtures/api/command/reschedule_task_command.json")
        def postResponse = getResource("/fixtures/api/reschedule_task_response.json")
        setupTaskCommand("http://localhost/tasks/1/2", expectedRequest, postResponse)

        when:
        def reschedule = store.reschedule(createTask(), triggerDate, Optional.of("reason1"))

        then:
        reschedule != null
        reschedule.id == new TaskId("1")
        reschedule.version == 2
        reschedule.queue == new QueueId("aggregator")
        reschedule.triggerDate == LocalDateTime.parse("2015-04-15T10:35:05", DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        reschedule.status == TaskStatus.WAITING
        reschedule.payload.getAsMap() == [type:"type1"]
    }

    def "should close task"()
    {
        given:
        def response = getResource("/fixtures/api/task.json")
        setupTaskRetrievalResponse("http://localhost/tasks/1", response)

        def expectedRequest = getResource("/fixtures/api/command/close_task_command.json")
        def postResponse = getResource("/fixtures/api/close_task_response.json")
        setupTaskCommand("http://localhost/tasks/1/2", expectedRequest, postResponse)

        when:
        def close = store.close(createTask(), Optional.of(CloseResultType.success), Optional.of("reason1"))

        then:
        close != null
        close.id == new TaskId("1")
        close.version == 2
        close.queue == new QueueId("aggregator")
        close.triggerDate == LocalDateTime.parse("2015-04-15T10:35:05", DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        close.status == TaskStatus.CLOSED
        close.payload.getAsMap() == [type:"type1"]
    }

    def "should cancel task"()
    {
        given:
        def response = getResource("/fixtures/api/task.json")
        setupTaskRetrievalResponse("http://localhost/tasks/1", response)

        def expectedRequest = getResource("/fixtures/api/command/cancel_task_command.json")
        def postResponse = getResource("/fixtures/api/cancel_task_response.json")
        setupTaskCommand("http://localhost/tasks/1/2", expectedRequest, postResponse)

        when:
        def cancel = store.cancel(createTask(), Optional.of("reason1"))

        then:
        cancel != null
        cancel.id == new TaskId("1")
        cancel.version == 2
        cancel.queue == new QueueId("aggregator")
        cancel.triggerDate == LocalDateTime.parse("2015-04-15T10:35:05", DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        cancel.status == TaskStatus.CANCELLED
        cancel.payload.getAsMap() == [type:"type1"]
    }

    private def setupTaskRetrievalResponse(final String expectedUrl, final String responseJson, final int status = 200)
    {
        StringEntity entity = new StringEntity(responseJson)
        StatusLine statusLine = new BasicStatusLine(new ProtocolVersion("HTTP", 1, 1), status, "OK")
        HttpResponse mockResponse = Mock(HttpResponse)
        mockResponse.getEntity() >> entity
        mockResponse.getStatusLine() >> statusLine

        client.execute({ request -> expectedUrl == request.getURI().toString()} as HttpUriRequest) >> mockResponse
    }

    private def setupRequestBuilder(final String expectedRequest, final String responseJson, final int status = 200)
    {
        final String expectedUrl = "http://localhost/tasks"
        StringEntity entity = new StringEntity(responseJson)
        StatusLine statusLine = new BasicStatusLine(new ProtocolVersion("HTTP", 1, 1), status, "OK")
        HttpResponse mockResponse = Mock(HttpResponse)
        mockResponse.getEntity() >> entity
        mockResponse.getStatusLine() >> statusLine

        client.execute({ request ->
            boolean foo = expectedUrl == request.getURI().toString() &&
            entityEqual(request.getEntity() as HttpEntity, expectedRequest)
            return foo
        } as HttpUriRequest) >> mockResponse
    }

    private static boolean entityEqual(HttpEntity entity, String expectedRequest)
    {
        def contentStream = entity.getContent()
        String content = CharStreams.toString(new InputStreamReader(contentStream, Charsets.UTF_8))
        Closeables.closeQuietly(contentStream)
        return jsonEqual(expectedRequest, content)
    }

    private static boolean jsonEqual(final String expectedJson, final String json)
    {
        return new JsonSlurper().parseText(expectedJson) == new JsonSlurper().parseText(json as String)
    }

    private def setupTaskCommand(final String expectedUrl, final String expectedRequest, final String responseJson, final int status = 200)
    {
        StringEntity entity = new StringEntity(responseJson)
        StatusLine statusLine = new BasicStatusLine(new ProtocolVersion("HTTP", 1, 1), status, "OK")
        HttpResponse mockResponse = Mock(HttpResponse)
        mockResponse.getEntity() >> entity
        mockResponse.getStatusLine() >> statusLine

        client.execute({ request ->
            expectedUrl == request.getURI().toString() &&
            entityEqual(request.getEntity() as HttpEntity, expectedRequest)
        } as HttpUriRequest) >> mockResponse
    }

    private static String getResource(String name)
    {
        return this.getClass().getResource(name).text
    }

    private static Task createTask()
    {
        return new Task(taskId, 2, queueId, TaskStatus.READY, created, triggerDate, attempts, payload)
    }
}
