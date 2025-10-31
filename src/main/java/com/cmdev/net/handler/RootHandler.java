package com.cmdev.net.handler;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import static com.cmdev.profiler.constants.Common.CONTENT_TYPE;
import static com.cmdev.profiler.constants.HttpStatus.OK;

public class RootHandler implements HttpHandler {

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        InputStream indexFile = this.getClass().getResourceAsStream("/statics/index.html");
        if (indexFile == null) {
            System.err.println("[CMDev] index.html not found!");
            exchange.sendResponseHeaders(404, 0);
            exchange.close();
            return;
        }
        byte[] fileBytes = indexFile.readAllBytes();
        Headers headers = exchange.getResponseHeaders();
        headers.set(CONTENT_TYPE, "text/html");
        exchange.sendResponseHeaders(OK, fileBytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(fileBytes);
        }
    }
}
