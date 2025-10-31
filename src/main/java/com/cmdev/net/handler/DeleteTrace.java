package com.cmdev.net.handler;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.File;

import static com.cmdev.profiler.constants.Common.TRACE_FILE_PATH;
import static com.cmdev.profiler.constants.HttpStatus.*;

public class DeleteTrace implements HttpHandler {

    @Override
    public void handle(HttpExchange exchange) {
        try {
            if (!"POST".equals(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(NOT_ALLOWED, -1);
                return;
            }
            String path = exchange.getRequestURI().getPath();
            String contextPath = "/api/trace/delete/";
            if (!path.startsWith(contextPath)) {
                exchange.sendResponseHeaders(BAD_REQUEST, -1);
                return;
            }
            String traceId = path.substring(contextPath.length());
            File directory = new File(TRACE_FILE_PATH);
            if (directory.exists() && directory.isDirectory()) {
                File[] files = directory.listFiles();
                if (files != null) {
                    java.util.Arrays.stream(files)
                        .filter(f -> f.isFile() && f.getName().contains(traceId + "|"))
                        .forEach(File::delete);
                }
            }
            exchange.sendResponseHeaders(OK, -1);
        } catch (Exception e) {
            try {
                exchange.sendResponseHeaders(500, -1);
            } catch (Exception ignored) {}
        }
    }
}
