package com.qmetric.penfold.client.app

import com.qmetric.penfold.client.app.support.Credentials
import com.qmetric.penfold.client.app.support.ObjectMapperFactory
import com.qmetric.penfold.client.domain.model.*
import com.sun.jersey.api.client.Client
import com.sun.jersey.api.client.ClientResponse
import com.sun.jersey.api.client.WebResource
import com.theoryinpractise.halbuilder.api.RepresentationFactory
import groovy.json.JsonSlurper
import spock.lang.Specification

import java.time.LocalDateTime

import static java.util.Optional.empty

class TaskStoreServiceImplTest extends Specification {

    static final taskId = new TaskId("1")

    static final queueId = new QueueId("q1")

    static final created = LocalDateTime.of(2014, 3, 15, 10, 35, 5)

    static final attempts = 0

    static final payload = new Payload([type: "type1"])

    static final triggerDate = LocalDateTime.of(2015, 4, 15, 10, 35, 5)

    final client = Mock(Client)

    final resourceBuilder = Mock(WebResource.Builder)

    final webResource = Mock(WebResource)

    final store = new TaskStoreServiceImpl("http://localhost", credentials(), client, ObjectMapperFactory.create())

    def "should create task"()
    {
        given:
        final response = response(201, "/fixtures/api/create_task_response.json")
        final resourceBuilder = setupRequestBuilder("http://localhost/tasks", CommandType.CreateTask)

        when:
        store.create(new NewTask(queueId, payload, empty()))

        then:
        1 * resourceBuilder.post(ClientResponse.class, _) >> { _, json ->
            assertExpectedJson(json as String, "/fixtures/api/command/create_task_command.json")
            response
        }
    }

    def "should create future task"()
    {
        given:
        final response = response(201, "/fixtures/api/create_future_task_response.json")
        final resourceBuilder = setupRequestBuilder("http://localhost/tasks", CommandType.CreateFutureTask)

        when:
        store.create(new NewTask(queueId, payload, Optional.of(triggerDate)))

        then:
        1 * resourceBuilder.post(ClientResponse.class, _) >> { _, json ->
            assertExpectedJson(json as String, "/fixtures/api/command/create_future_task_command.json")
            response
        }
    }

    def "should start task"()
    {
        given:
        setupTaskRetrievalResponse("/fixtures/api/task.json")
        setupTaskCommand("http://localhost/tasks/1/2", CommandType.StartTask)
        final postResponse = response(200, "/fixtures/api/start_task_response.json")

        when:
        store.start(createTask())

        then:
        1 * resourceBuilder.post(ClientResponse.class, _) >> { _, json ->
            assertExpectedJson(json as String, "/fixtures/api/command/start_task_command.json")
            postResponse
        }
    }

    def "should requeue task"()
    {
        given:
        setupTaskRetrievalResponse("/fixtures/api/task.json")
        setupTaskCommand("http://localhost/tasks/1/2", CommandType.RequeueTask)
        final postResponse = response(200, "/fixtures/api/requeue_task_response.json")

        when:
        store.requeue(createTask(), Optional.of("reason1"))

        then:
        1 * resourceBuilder.post(ClientResponse.class, _) >> { _, json ->
            assertExpectedJson(json as String, "/fixtures/api/command/requeue_task_command.json")
            postResponse
        }
    }

    def "should reschedule task"()
    {
        given:
        setupTaskRetrievalResponse("/fixtures/api/task.json")
        setupTaskCommand("http://localhost/tasks/1/2", CommandType.RescheduleTask)
        final postResponse = response(200, "/fixtures/api/reschedule_task_response.json")

        when:
        store.reschedule(createTask(), triggerDate, Optional.of("reason1"))

        then:
        1 * resourceBuilder.post(ClientResponse.class, _) >> { _, json ->
            assertExpectedJson(json as String, "/fixtures/api/command/reschedule_task_command.json")
            postResponse
        }
    }

    def "should close task"()
    {
        given:
        setupTaskRetrievalResponse("/fixtures/api/task.json")
        setupTaskCommand("http://localhost/tasks/1/2", CommandType.CloseTask)
        final postResponse = response(200, "/fixtures/api/close_task_response.json")

        when:
        store.close(createTask(), Optional.of(CloseResultType.success), Optional.of("reason1"))

        then:
        1 * resourceBuilder.post(ClientResponse.class, _) >> { _, json ->
            assertExpectedJson(json as String, "/fixtures/api/command/close_task_command.json")
            postResponse
        }
    }

    def "should cancel task"()
    {
        given:
        setupTaskRetrievalResponse("/fixtures/api/task.json")
        setupTaskCommand("http://localhost/tasks/1/2", CommandType.CancelTask)
        final postResponse = response(200, "/fixtures/api/cancel_task_response.json")

        when:
        store.cancel(createTask(), Optional.of("reason1"))

        then:
        1 * resourceBuilder.post(ClientResponse.class, _) >> { _, json ->
            assertExpectedJson(json as String, "/fixtures/api/command/cancel_task_command.json")
            postResponse
        }
    }

    private def setupTaskRetrievalResponse(final String json, final int status = 200)
    {
        final ClientResponse response = response(status)
        response.getEntityInputStream() >> this.getClass().getResource(json).newInputStream()
        final resourceBuilder = Mock(WebResource.Builder)
        final webResource = Mock(WebResource)
        client.resource("http://localhost/tasks/${taskId}") >> webResource
        webResource.accept(RepresentationFactory.HAL_JSON) >> resourceBuilder
        resourceBuilder.get(ClientResponse.class) >> response
        response
    }

    private void assertExpectedJson(final String json, final String expectedJson)
    {
        assert new JsonSlurper().parseText(json as String) == new JsonSlurper().parseText(this.getClass().getResource(expectedJson).text)
    }

    private def setupRequestBuilder(final String url, final CommandType commandType)
    {
        final resourceBuilder = Mock(WebResource.Builder)
        final webResource = Mock(WebResource)
        client.resource(url) >> webResource
        webResource.accept(RepresentationFactory.HAL_JSON) >> resourceBuilder
        resourceBuilder.type(contentType(commandType)) >> resourceBuilder
        resourceBuilder
    }

    private def response(final statusCode)
    {
        final response = Mock(ClientResponse)
        response.getStatus() >> statusCode
        response
    }

    private def response(final statusCode, final String jsonPath)
    {
        final response = response(statusCode)
        response.getEntityInputStream() >> this.getClass().getResource(jsonPath).newInputStream()
        response
    }

    private def setupTaskCommand(final String url, final CommandType commandType)
    {
        client.resource(url) >> webResource
        webResource.accept(RepresentationFactory.HAL_JSON) >> resourceBuilder
        resourceBuilder.type(contentType(commandType)) >> resourceBuilder
    }

    private static def contentType(final CommandType commandType)
    {
        return "application/json;domain-command=" + commandType.name()
    }

    private static Credentials credentials()
    {
        return new Credentials("user", "pwd")
    }

    private static Task createTask()
    {
        return new Task(taskId, "2", queueId, TaskStatus.READY, created, attempts, payload)
    }
}
