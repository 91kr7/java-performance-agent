package com.cmdev.net.handler;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;

public class StaticResourceHandler implements HttpHandler {

    public StaticResourceHandler() {
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        System.out.println("[StaticResourceHandler] Requested path: " + path);
        if (path.startsWith("/")) {
            path = path.substring(1);
        }
        String resourcePath = "/" + path;
        InputStream is = this.getClass().getResourceAsStream(resourcePath);
        if (is == null) {
            System.out.println("[StaticResourceHandler] Resource not found: " + resourcePath);
            exchange.sendResponseHeaders(404, -1);
            return;
        }
        String contentType = getContentType(path);
        byte[] bytes = is.readAllBytes();
        System.out.println("[StaticResourceHandler] Serving: " + resourcePath + " | Content-Type: " + contentType + " | Size: " + bytes.length + " bytes");
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.sendResponseHeaders(200, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private String getContentType(String path) {
        if (path.endsWith(".js")) return "application/javascript";
        if (path.endsWith(".css")) return "text/css";
        if (path.endsWith(".html")) return "text/html";
        return "application/octet-stream";
    }
}
