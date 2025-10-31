package com.cmdev.net.handler;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static com.cmdev.profiler.constants.Common.CONTENT_TYPE;
import static com.cmdev.profiler.constants.Common.TRACE_FILE_PATH;
import static com.cmdev.profiler.constants.HttpStatus.NOT_ALLOWED;
import static com.cmdev.profiler.constants.HttpStatus.OK;

public class GetTraces implements HttpHandler {

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if ("GET".equals(exchange.getRequestMethod())) {

            File directory = new File(TRACE_FILE_PATH);
            List<String> files = new ArrayList<>();
            if (directory.exists() && directory.isDirectory()) {
                for (File file : Objects.requireNonNull(directory.listFiles())) {
                    if (file.isFile() && file.getName().toLowerCase().endsWith(".gz")) {
                        files.add(file.getName());
                    }
                }
            }

            Headers headers = exchange.getResponseHeaders();
            headers.set(CONTENT_TYPE, "application/json");

            byte[] bytes = convertFilesListToJson(files).getBytes(StandardCharsets.UTF_8);

            exchange.sendResponseHeaders(OK, bytes.length);
            OutputStream os = exchange.getResponseBody();
            os.write(bytes);
            os.close();
        } else {
            // Method Not Allowed: the request method is not GET.
            exchange.sendResponseHeaders(NOT_ALLOWED, -1);
        }
    }

    public String convertFilesListToJson(List<String> files) {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (int i = 0; i < files.size(); i++) {
            String escaped = files.get(i).replace("\"", "\\\"");
            sb.append("\"").append(escaped).append("\"");
            if (i < files.size() - 1) {
                sb.append(",");
            }
        }
        sb.append("]");
        return sb.toString();
    }
}
