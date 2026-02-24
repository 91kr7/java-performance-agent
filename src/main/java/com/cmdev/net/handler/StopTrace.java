package com.cmdev.net.handler;

import com.cmdev.profiler.instrument.TracingGlobalStatus;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import static com.cmdev.profiler.constants.HttpStatus.NOT_ALLOWED;
import static com.cmdev.profiler.constants.HttpStatus.OK;

public class StopTrace implements HttpHandler {

    @Override
    public void handle(HttpExchange exchange) {

        try {
            if (!"POST".equals(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(NOT_ALLOWED, -1);
                return;
            }
            TracingGlobalStatus.systemInstrumentationEnabled = false;
            TracingGlobalStatus.packageToExclude = null;
            TracingGlobalStatus.methodToTrace = null;
            exchange.sendResponseHeaders(OK, -1);
        } catch (Exception e) {
            try {
                exchange.sendResponseHeaders(500, -1);
            } catch (Exception ignored) {}
        }
    }
}
