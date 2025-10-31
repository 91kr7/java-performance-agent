package com.cmdev.net.handler;

import com.cmdev.profiler.instrument.TimerContext;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.sql.Time;

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
            TimerContext.systemInstrumentationEnabled = false;
            TimerContext.packageToExclude = null;
            TimerContext.methodToTrace = null;
            exchange.sendResponseHeaders(OK, -1);
        } catch (Exception e) {
            try {
                exchange.sendResponseHeaders(500, -1);
            } catch (Exception ignored) {}
        }
    }
}
