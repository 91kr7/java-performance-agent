# Java Performance Agent

## Introduction
Java Performance Agent is a lightweight, pluggable Java agent designed to monitor, trace, and analyze the performance of Java applications in real time. It provides actionable insights into method execution times, call stacks, and resource usage, helping to identify bottlenecks and optimize application performance.

## Overview
Enables real-time performance monitoring and tracing for Java applications, helping to identify bottlenecks and optimize code execution.

## Architecture
- **AgentEntrypoint & AgentInitializer**: Bootstrap and configure the agent at JVM startup.
- **AgentStarter & AgentStarterImpl**: Manage the lifecycle and instrumentation logic.
- **PerformanceTracer & TimerContext**: Collect and aggregate timing data for instrumented methods.
- **Instrumentation via ByteBuddy**: Uses ByteBuddy to dynamically instrument classes and methods at runtime, enabling precise and flexible performance tracing without modifying application code.
- **ManagementHttpServer**: Embedded HTTP server exposing REST APIs and a web interface for trace management and visualization.
- **Handlers (GetTrace, GetTraces, DeleteTrace, StopTrace, etc.)**: REST endpoints for trace operations (start, stop, list, download, delete).
- **StaticResourceHandler**: Serves the web UI (HTML, CSS, JS) for interactive trace exploration.
- **File Writers & Utilities**: Serialize, compress, and store trace data efficiently.

## Requirements
- Java 17+ (compatible with modern JVMs)
- Works with Tomcat, WildFly, and other servlet containers
- No code changes required in the target application

## Third-party Libraries & Docker Images
This project uses [ByteBuddy](https://bytebuddy.net/) for runtime instrumentation of Java classes and methods.
- ByteBuddy is licensed under the Apache License, Version 2.0. [License](https://www.apache.org/licenses/LICENSE-2.0)
- By using ByteBuddy, this project complies with the terms of the Apache License 2.0, including attribution and license notice.

This project uses official Docker images for:
- [Apache Tomcat](https://hub.docker.com/_/tomcat) (Apache License 2.0) [License](https://www.apache.org/licenses/LICENSE-2.0)
- [Wildfly](https://hub.docker.com/r/jboss/wildfly) (LGPL v2.1) [License](https://www.gnu.org/licenses/old-licenses/lgpl-2.1.html)

By using these images, you accept the terms of their respective licenses.

## Usage & Integration
- **Attach to JVM**: Add the agent JAR to your JVM startup parameters:
  ```sh
  -javaagent:/path/to/performance-agent.jar -Dcmdev.profiler.filters.path=/path/to/filters.properties
  ```
- **Configure Filters**: Use `filters.properties` to specify which classes/methods to instrument.
- **Access Web UI**: Open the embedded HTTP server (default port 8090) to interact with traces and view results.
- **API Endpoints**: The REST endpoints are primarily called by the web UI for trace management and visualization, but are also available for automation and integration with CI/CD or monitoring tools.

## How It Works
1. **Startup**: The agent is attached to the JVM (via `-javaagent`), initializing its configuration and HTTP server.
2. **Instrumentation**: Classes and methods are instrumented based on filters and configuration, enabling precise timing and call tracking.
3. **Trace Collection**: When a trace is started (via API or UI), the agent records method execution data and call stacks.
4. **Trace Management**: Traces can be listed, downloaded, deleted, or stopped via REST API or the web interface.
5. **Analysis**: Collected traces are available for download and analysis, helping to pinpoint performance issues.

## Example REST Endpoints
- `GET /traces` – List available traces
- `POST /trace/start` – Start a new trace
- `POST /trace/stop` – Stop the current trace
- `GET /trace/{id}` – Download a trace file
- `DELETE /trace/{id}` – Delete a trace

## Quick Start
1. Build the agent and your target application.
2. Start your application with the agent attached.
3. Access the web UI or use the REST API to manage and analyze traces.

## How to Start
To run and test the Performance Agent with Tomcat or Wildfly, follow these steps:

### Tomcat 10.0 jdk17
```bash
export MSYS_NO_PATHCONV=1
JAVA_TOOL_OPTIONS="-XX:-UseContainerSupport -javaagent:/usr/local/tomcat/performance-agent.jar -Dcmdev.profiler.filters.path=/usr/local/tomcat/filters.properties"
mvn -q clean install
rootDir=$PWD
pushd test-app
mvn -q clean install
docker build -t test-app-tomcat -f Dockerfile-Tomcat10jdk17 .
docker run -p 8080:8080 -p 8090:8090 -p 8787:8787 \
    -e JAVA_TOOL_OPTIONS="${JAVA_TOOL_OPTIONS}" \
    -v $rootDir/test-app/filters.properties:/usr/local/tomcat/filters.properties \
    -v $rootDir/target/performance-agent.jar:/usr/local/tomcat/performance-agent.jar \
    -v $rootDir/test-app/traces:/tmp/traces \
    test-app-tomcat
popd
```

### Wildfly 28.0.0.Final-jdk17
```bash
export MSYS_NO_PATHCONV=1
JAVA_TOOL_OPTIONS=" -javaagent:/opt/jboss/wildfly/performance-agent.jar  -Dcmdev.profiler.filters.path=/opt/jboss/wildfly/filters.properties"
mvn -q clean install
rootDir=$PWD
pushd test-app
mvn -q clean install
docker build -t test-app-wildfly -f Dockerfile-Wildfly28jdk17 .
docker run -p 8080:8080 -p 8090:8090 -p 8787:8787 \
    -e JAVA_OPTS="${JAVA_TOOL_OPTIONS}" \
    -v $rootDir/test-app/filters.properties:/opt/jboss/wildfly/filters.properties \
    -v $rootDir/target/performance-agent.jar:/opt/jboss/wildfly/performance-agent.jar \
    -v $rootDir/test-app/traces:/tmp/traces \
    test-app-wildfly
popd
```

### JDK 17 
```bash
export MSYS_NO_PATHCONV=1
JAVA_TOOL_OPTIONS="-XX:-UseContainerSupport -javaagent:/usr/src/app/performance-agent.jar -Dcmdev.profiler.filters.path=/usr/src/app/filters.properties"
mvn -q clean install
rootDir=$PWD
pushd test-app
mvn -q clean install
docker build -t test-app-jdk -f Dockerfile-jdk17 .
docker run -p 8090:8090 -p 8787:8787 \
    -e JAVA_TOOL_OPTIONS="${JAVA_TOOL_OPTIONS}" \
    -v $rootDir/test-app/filters.properties:/usr/src/app/filters.properties \
    -v $rootDir/target/performance-agent.jar:/usr/src/app/performance-agent.jar \
    -v $rootDir/test-app/traces:/tmp/traces \
    test-app-jdk
popd
```

## License & Contributions
Open source project – contributions and feedback are welcome!
