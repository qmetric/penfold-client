# penfold-client

[![Build Status](https://travis-ci.org/qmetric/penfold-client.png)](https://travis-ci.org/qmetric/penfold-client)

Java client for [penfold](https://github.com/qmetric/penfold).

## Prerequisites:

* [JVM](https://www.java.com/en/download/) 8+
* A running [penfold](https://github.com/qmetric/penfold) server


## Usage

#### Add dependency to your project:

```
<dependency>
    <groupId>com.qmetric</groupId>
    <artifactId>penfold-client</artifactId>
    <version>${VERSION}</version>
</dependency>
```

#### Configure a query service:

Use this if you wish to query tasks.

```java
final TaskQueryService service = new TaskQueryServiceBuilder()
    .forServer("http://localhost")
    .withCredentials("user", "pass")
    .build();
```


#### Configure a store:

Use this if you wish to create or update tasks.

```java
final TaskStoreService service = new TaskStoreServiceBuilder()
    .forServer("http://localhost")
    .withCredentials("user", "pass")
    .build();
```


#### Configure and start a consumer:

Use this if you wish to consume from a queue of tasks.

```java
new TaskConsumerBuilder()
    .fromServer("http://localhost")
    .withCredentials("user", "pass")
    .fromQueue("testqueue")
    .delayBetweenEachRetryOf(Duration.ofMinutes(15))
    .withActivityHealthCheck(Duration.ofMinutes(30), existingHealthCheckRegistry)
    .consumeWith(new ConsumerFunction() {
        @Override public Reply execute(final Task task) {
            // your implementation here
        }})
    .build()
    .start();
```


#### Connectivity health check:

Penfold connectivity health check can be appended to an existing com.codahale.metrics.health.HealthCheckRegistry.

```java
final HealthCheckRegistry updated = new PenfoldServerConnectivityHealthCheckConfigurer("http://localhost", existingHealthCheckRegistry).configure()
```
