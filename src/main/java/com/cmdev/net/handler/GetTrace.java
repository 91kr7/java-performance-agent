package com.cmdev.net.handler;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static com.cmdev.profiler.constants.Common.CONTENT_TYPE;
import static com.cmdev.profiler.constants.Common.TRACE_FILE_PATH;
import static com.cmdev.profiler.constants.HttpStatus.*;

public class GetTrace implements HttpHandler {

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!"GET".equals(exchange.getRequestMethod())) {
            System.err.println("[CMDev] Method not allowed: " + exchange.getRequestMethod());
            exchange.sendResponseHeaders(NOT_ALLOWED, -1);
            return;
        }
        String path = exchange.getRequestURI().getPath();
        String contextPath = "/api/trace/";

        if (!path.startsWith(contextPath)) {
            System.err.println("[CMDev] Path not valid: " + path);
            exchange.sendResponseHeaders(BAD_REQUEST, -1);
            return;
        }

        String fileId = path.substring(contextPath.length());
        if (fileId.isEmpty()) {
            System.err.println("[CMDev] fileId missing");
            exchange.sendResponseHeaders(BAD_REQUEST, -1);
            return;
        }

        Path baseDir = Path.of(TRACE_FILE_PATH);
        Path filePath = baseDir.resolve(fileId);

        if (!Files.exists(filePath) || !Files.isRegularFile(filePath)) {
            System.err.println("[CMDev] Not Found: " + filePath);
            exchange.sendResponseHeaders(NOT_FOUND, -1);
            return;
        }

        byte[] gzBytes;
        String fileName = fileId;
        try {
            gzBytes = Files.readAllBytes(filePath.toAbsolutePath());
            exchange.getResponseHeaders().set(CONTENT_TYPE, "application/gzip");
            exchange.getResponseHeaders().set("Content-Encoding", "gzip");
            exchange.getResponseHeaders().set("Content-Disposition", "attachment; filename=\"" + fileName + "\"");
            exchange.getResponseHeaders().set("Content-Length", String.valueOf(gzBytes.length));
            exchange.sendResponseHeaders(OK, gzBytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(gzBytes);
            }
        } catch (Exception e) {
            System.err.println("[CMDev] Error writing response: " + e.getMessage());
            exchange.sendResponseHeaders(INTERNAL_SERVER_ERROR, -1);
        }
    }
}
