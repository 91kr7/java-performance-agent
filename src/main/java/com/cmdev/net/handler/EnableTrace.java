package com.cmdev.net.handler;

import com.cmdev.profiler.instrument.TracingGlobalStatus;
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
                TracingGlobalStatus.systemInstrumentationEnabled = true;
                TracingGlobalStatus.methodToTrace = qs.get("expression");
                TracingGlobalStatus.packageToExclude = null;
                if (qs.containsKey("packagesFilter")) {
                    String packagesFilter = qs.get("packagesFilter");
                    if (packagesFilter != null && !packagesFilter.trim().isEmpty()) {
                        java.util.Set<String> packageSet = new java.util.HashSet<>();
                        for (String pkg : packagesFilter.split(",")) {
                            if (!pkg.trim().isEmpty()) {
                                packageSet.add(pkg.trim());
                            }
                        }
                        TracingGlobalStatus.packageToExclude = packageSet;
                    } else {
                        TracingGlobalStatus.packageToExclude = null;
                    }
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
