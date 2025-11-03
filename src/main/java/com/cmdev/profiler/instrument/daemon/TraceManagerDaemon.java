package com.cmdev.profiler.instrument.daemon;

import com.cmdev.profiler.instrument.io.PerformanceFileWriter;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class TraceManagerDaemon extends Thread {

    private final ConcurrentLinkedQueue<String> traceQueue = new ConcurrentLinkedQueue<>();
    private static final Map<String, PerformanceFileWriter> outputBuffer = new ConcurrentHashMap<>();

    @Override
    public void run() {

        while (!Thread.currentThread().isInterrupted()) {
            String item = traceQueue.poll();
            if (item != null) {

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


}
