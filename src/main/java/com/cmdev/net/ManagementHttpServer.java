package com.cmdev.net;

import com.cmdev.net.handler.*;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;

import static com.cmdev.profiler.constants.Common.PORT;

public class ManagementHttpServer {

    public void run() throws IOException {

        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);

        server.createContext("/", new RootHandler());
        server.createContext("/statics/", new StaticResourceHandler());

        server.createContext("/api/traces", new GetTraces());
        server.createContext("/api/trace", new GetTrace());
        server.createContext("/api/trace/start", new EnableTrace());
        server.createContext("/api/trace/stop", new StopTrace());
        server.createContext("/api/trace/setup", new GetTraceInfo());
        server.createContext("/api/trace/delete", new DeleteTrace());

        server.start();
        System.out.println("Performance Server started at port " + PORT);
    }
}