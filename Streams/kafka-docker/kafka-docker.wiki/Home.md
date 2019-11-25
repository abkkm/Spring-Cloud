FAQs
----

### Why can I not connect to the Kafka broker?

This is a very common issue. The majority of the time this is due to configuration - please make sure you understand the Kafka networking requirements. Please refer to the [Connectivity Guide](./Connectivity.md) for a brief overview.

### Why is ./var/run.docker.sock needed?

It is needed to access the hosts Docker daemon from inside the container, i.e. 'Docker in Docker'. There are several places where this is used, none are required.

However, if this is omitted, the `kafka-start.sh` script will no longer be able to automatically determine the `KAFKA_ADVERTISED_PORT`. If this is required, please specify it manually in the environment.

### Why do Kafka tools fail when JMX is enabled?

**Problem**: Tools such as `kafka-topics.sh` and `kafka-console-producer.sh` fail when JMX is enabled. This is caused because of the `JMX_PORT` environment variable. The Kafka helper script `/opt/kafka/bin/kafka-run-class.sh` will try to invoke the required command in a new JVM with JMX bound to the specified port. As the broker JVM that is already running in the container has this port bound, the process fails and exits with error.

**Solution**: Although we'd recommend not running operation tools inside of your running brokers, this may sometimes be desirable when performing local development and testing.

Either prefix your command with `JMX_PORT= <command>` or unset the environment variable, i.e. `unset JMX_PORT`.

```
JMX_PORT= ./kafka-console-producer.sh --broker-list localhost:9092 --topic test
```

### How can I connect to the running container?

Use `docker exec`, i.e

```
docker exec -it kafkadocker_kafka_1
```

### On Windows "\\\\var\\\\run\\\\docker.sock" can't be mounted - Is there anything I can do?

It's been reported that [COMPOSE_CONVERT_WINDOWS_PATHS](https://docs.docker.com/compose/reference/envvars/#compose_convert_windows_paths) fixes this issue.

Edit your local .envs file and set that parameter to `true` or `1`

```
echo "COMPOSE_CONVERT_WINDOWS_PATH=1" >> .env
```

### Topic Compaction does not work

Please check the canonical Kafka documentation for your specific version of Kafka: [https://kafka.apache.org/documentation/#compaction](https://kafka.apache.org/documentation/#compaction)

In most versions of Kafka, log-compaction can be configured globally and/or per-topic. The default broker-wide config is `log.cleaner.enable=true` and `log.cleanup.policy=compact` - meaning compaction is enabled by default. Topics can override the broker cleanup policy using: `cleanup.policy=[compact|delete]`.

Other relevant broker configurations to consider are:

-	`log.cleaner.min.cleanable.ratio` - (default 50%) The minimum ratio of dirty log to total log for a log to eligible for cleaning.
-	`log.cleaner.min.compaction.lag.ms` - (default 0) The minimum time a message will remain uncompacted in the log. Only applicable for logs that are being compacted.

Even after configuring the above settings, with a smaller set of data, log-compaction may never occur because ([ref](https://kafka.apache.org/documentation/#design_compactionconfig)\):

> The active segment will not be compacted even if all of its messages are older than the minimum compaction time lag.

Here it is important to understand that the size of log segments are controlled with `log.segment.bytes` - default `1GB` (topic and/or broker config). If the log-segment takes a long time to fill up (or never fills up) then compaction will not occur.
