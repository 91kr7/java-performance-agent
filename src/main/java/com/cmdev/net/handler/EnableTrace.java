package com.cmdev.net.handler;

import com.cmdev.profiler.instrument.TimerContext;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.cmdev.net.util.QueryStringUtils;

import java.io.IOException;
import java.util.Map;

import static com.cmdev.profiler.constants.HttpStatus.*;

public class EnableTrace implements HttpHandler {

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            if (!"POST".equals(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(NOT_ALLOWED, -1);
                return;
            }
            Map<String, String> qs = QueryStringUtils.parse(exchange.getRequestURI().getQuery());
            if (qs.containsKey("expression")) {
                TimerContext.systemInstrumentationEnabled = true;
                TimerContext.methodToTrace = qs.get("expression");
                TimerContext.packageToExclude = null;
                if (qs.containsKey("packagesFilter")) {
                    String packagesFilter = qs.get("packagesFilter");
                    TimerContext.packageToExclude = packagesFilter != null && packagesFilter.trim().length() > 0 ? packagesFilter.split(",") : null;
                }
                exchange.sendResponseHeaders(OK, -1);
            } else {
                exchange.sendResponseHeaders(BAD_REQUEST, -1);
            }
        } catch (Exception e) {
            exchange.sendResponseHeaders(500, -1);
        }
    }
}
