package com.cmdev.net.handler;

import com.cmdev.profiler.instrument.TracingGlobalStatus;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

import static com.cmdev.profiler.constants.Common.CONTENT_TYPE;
import static com.cmdev.profiler.constants.HttpStatus.NOT_ALLOWED;
import static com.cmdev.profiler.constants.HttpStatus.OK;

public class GetTraceInfo implements HttpHandler {

    @Override
    public void handle(HttpExchange exchange) throws IOException {

        if ("GET".equals(exchange.getRequestMethod())) {

            Headers headers = exchange.getResponseHeaders();
            headers.set(CONTENT_TYPE, "application/json");

            byte[] bytes = ("{\"enabled\": " + TracingGlobalStatus.systemInstrumentationEnabled
                    + ", \"packagesFilter\": \"" + (TracingGlobalStatus.packageToExclude != null ? String.join(",", TracingGlobalStatus.packageToExclude) : "") + "\""
                    + ", \"expression\": \"" + (TracingGlobalStatus.methodToTrace != null ? TracingGlobalStatus.methodToTrace : "") + "\"}")
                    .getBytes(StandardCharsets.UTF_8);

            exchange.sendResponseHeaders(OK, bytes.length);
            OutputStream os = exchange.getResponseBody();
            os.write(bytes);
            os.close();
        } else {
            exchange.sendResponseHeaders(NOT_ALLOWED, -1);
        }
    }
}
