package com.cmdev.profiler.instrument.daemon;

import com.cmdev.profiler.instrument.TraceMessage;
import com.cmdev.profiler.instrument.io.PerformanceFileWriter;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class TraceManagerDaemon extends Thread {

    private static final String OUTPUTDIR = "/tmp/traces/";

    private static final ConcurrentLinkedQueue<TraceMessage> traceQueue = new ConcurrentLinkedQueue<>();
    private static final Map<String, PerformanceFileWriter> outputBuffer = new ConcurrentHashMap<>();

    public static void putEntry(TraceMessage trace) {
        traceQueue.offer(trace);
    }

    private static String intendMessage(int depth, String message) {
        if (depth < 0) {
            depth = 0;
        }
        String indent = " ".repeat(depth * 2);
        return indent + message;
    }

    @Override
    public void run() {

        while (true) {
            TraceMessage trace = traceQueue.poll();
            if (trace != null) {
                processEntry(trace);
            } else {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
    }

    private void processEntry(TraceMessage trace) {

        try {
            PerformanceFileWriter writer = outputBuffer.computeIfAbsent(trace.getThreadId(), id -> new PerformanceFileWriter(OUTPUTDIR + trace.getThreadId()));
            writer.writeLine(intendMessage(trace.getDepthOfTheMessage(), trace.getPrefix() + trace.getMessage()));
            if (trace.isTraceEnded()) {
                writer.close();
                outputBuffer.remove(trace.getThreadId());
            }
        } catch (Exception e) {
            System.err.println("[CMDev] Error while processing trace message " + trace.getDepthOfTheMessage() + ": " + e.getMessage());
        }
    }
}
