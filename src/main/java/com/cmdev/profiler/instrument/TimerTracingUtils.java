package com.cmdev.profiler.instrument;

import com.cmdev.profiler.instrument.io.PerformanceFileWriter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

public class TimerTracingUtils {

    private final static String OUTPUTDIR = "/tmp/traces/";
    private final static String TRACE_INDENT_ON = "+ ";
    private final static String TRACE_DELIMITER_OFF = "- ";


    private static final Map<String, AtomicInteger> callDeep = new ConcurrentHashMap<>();

    private final ConcurrentLinkedQueue<String> traceQueue = new ConcurrentLinkedQueue<>();
    private static final Map<String, PerformanceFileWriter> outputBuffer = new ConcurrentHashMap<>();


    private static String getThreadId() {
        return TimerContext.getTraceId();
    }

    public static void tryToTrace(String message, boolean isMethodStart) {
        try {
            if (TimerContext.systemInstrumentationEnabled) {
                if ((TimerContext.isTraceEnabled() ||
                        message.contains(TimerContext.methodToTrace))
                        && !containsExcludedPackage(message, TimerContext.packageToExclude)) {
                    if (!TimerContext.isTraceEnabled()) {
                        TimerContext.initTrace(message);
                    }
                    trace(message, isMethodStart);
                }
            }
        } catch (Throwable e) {
            System.err.println("[CMDev] TimerTracingUtils: " + e.getMessage());
        }
    }

    private static boolean containsExcludedPackage(String message, String[] packagesToExclude) {
        if (packagesToExclude == null) return false;
        for (String pkg : packagesToExclude) {
            if (pkg != null && message.contains(pkg)) {
                return true;
            }
        }
        return false;
    }

    public static void trace(String message, boolean isMethodStart) throws IOException {

        String threadId = getThreadId();
        if (threadId != null) {

            AtomicInteger depth = callDeep.computeIfAbsent(threadId, id -> new AtomicInteger(0));

            int depthOfTheMessage = -1;
            if (isMethodStart) {
                depthOfTheMessage = depth.getAndIncrement();
            } else {
                depthOfTheMessage = depth.decrementAndGet();
                if (depthOfTheMessage < 0) {
                    depthOfTheMessage = 0;
                    depth.set(0);
                }
            }
            boolean traceEnded = callDeep.get(threadId).get() == 0;
            String prefix = isMethodStart ? TRACE_INDENT_ON : TRACE_DELIMITER_OFF;
            // put the mssage in the queue

            PerformanceFileWriter writer = outputBuffer.computeIfAbsent(threadId, id -> new PerformanceFileWriter(OUTPUTDIR + threadId));

            writer.writeLine(intendMessage(depthOfTheMessage, prefix + message));

            if (traceEnded) {
                TimerContext.stopTrace();
                callDeep.remove(threadId);
                writer.close();
                outputBuffer.remove(threadId);
            }
        }
    }

    private static String intendMessage(int depth, String message) {
        if (depth < 0) {
            depth = 0;
        }
        String indent = " ".repeat(depth * 2);
        return indent + message;
    }
}
