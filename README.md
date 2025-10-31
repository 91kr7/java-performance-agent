# Java Performance Agent – Introduction

## Overview

Java Performance Agent is a lightweight, pluggable Java agent designed to monitor, trace, and analyze the performance of Java applications in real time. It provides developers and operators with actionable insights into method execution times, call stacks, and resource usage, helping to identify bottlenecks and optimize application performance.

## Architecture

The agent consists of several key components:
- **AgentEntrypoint & AgentInitializer**: Bootstrap and configure the agent at JVM startup.
- **AgentStarter & AgentStarterImpl**: Manage the lifecycle and instrumentation logic.
- **PerformanceTracer & TimerContext**: Collect and aggregate timing data for instrumented methods.
- **Instrumentation via ByteBuddy**: The agent uses ByteBuddy to dynamically instrument classes and methods at runtime, enabling precise and flexible performance tracing without modifying application code.
- **ManagementHttpServer**: Embedded HTTP server exposing REST APIs and a web interface for trace management and visualization.
- **Handlers (GetTrace, GetTraces, DeleteTrace, StopTrace, etc.)**: REST endpoints for trace operations (start, stop, list, download, delete).
- **StaticResourceHandler**: Serves the web UI (HTML, CSS, JS) for interactive trace exploration.
- **File Writers & Utilities**: Serialize, compress, and store trace data efficiently.

## How It Works

1. **Startup**: The agent is attached to the JVM (via `-javaagent`), initializing its configuration and HTTP server.
2. **Instrumentation**: Classes and methods are instrumented based on filters and configuration, enabling precise timing and call tracking.
3. **Trace Collection**: When a trace is started (via API or UI), the agent records method execution data and call stacks.
4. **Trace Management**: Traces can be listed, downloaded, deleted, or stopped via REST API or the web interface.
5. **Analysis**: Collected traces are available for download and analysis, helping to pinpoint performance issues.

## Usage & Integration

- **Attach to JVM**: Add the agent JAR to your JVM startup parameters:
  ```sh
  -javaagent:/path/to/performance-agent.jar
  ```
- **Configure Filters**: Use `filters.properties` to specify which classes/methods to instrument.
- **Access Web UI**: Open the embedded HTTP server (default port 8090) to interact with traces and view results.
- **API Endpoints**: The REST endpoints are primarily called by the web UI for trace management and visualization, but are also available for automation and integration with CI/CD or monitoring tools.

## Example REST Endpoints
- `GET /traces` – List available traces
- `POST /trace/start` – Start a new trace
- `POST /trace/stop` – Stop the current trace
- `GET /trace/{id}` – Download a trace file
- `DELETE /trace/{id}` – Delete a trace

## Requirements
- Java 17+ (compatible with modern JVMs)
- Works with Tomcat, WildFly, and other servlet containers
- No code changes required in the target application

## Quick Start
1. Build the agent and your target application.
2. Start your application with the agent attached.
3. Access the web UI or use the REST API to manage and analyze traces.

## License & Contributions
Open source project – contributions and feedback are welcome!

## Third-party libraries

This project uses [ByteBuddy](https://bytebuddy.net/) for runtime instrumentation of Java classes and methods.

ByteBuddy is licensed under the Apache License, Version 2.0. You can find the full license text at: https://www.apache.org/licenses/LICENSE-2.0

By using ByteBuddy, this project complies with the terms of the Apache License 2.0, including attribution and license notice.
