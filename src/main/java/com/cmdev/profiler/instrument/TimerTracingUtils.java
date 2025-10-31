package com.cmdev.profiler.instrument;

import com.cmdev.profiler.instrument.io.PerformanceFileWriter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class TimerTracingUtils {

    private final static String OUTPUTDIR = "/tmp/traces/";
    private final static String TRACE_INDENT_ON = "+ ";
    private final static String TRACE_DELIMITER_OFF = "- ";


    private static final Map<String, AtomicInteger> callDeep = new ConcurrentHashMap<>();
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
            PerformanceFileWriter writer = outputBuffer.computeIfAbsent(threadId, id -> new PerformanceFileWriter(OUTPUTDIR + threadId));

            writer.writeLine(buildMessage(threadId, message, isMethodStart));

            AtomicInteger depth = callDeep.computeIfAbsent(threadId, id -> new AtomicInteger(0));
            if (depth.get() == 0) {
                TimerContext.stopTrace();
                callDeep.remove(threadId);
                writer.close();
                outputBuffer.remove(threadId);
            }
        }
    }

    private static String buildMessage(String threadId, String message, boolean isMethodStart) {
        AtomicInteger depth = callDeep.computeIfAbsent(threadId, id -> new AtomicInteger(0));

        int currentDepth;
        if (isMethodStart) {
            currentDepth = depth.getAndIncrement();
        } else {
            currentDepth = depth.decrementAndGet();
            if (currentDepth < 0) {
                currentDepth = 0;
                depth.set(0);
            }
        }

        String prefix = isMethodStart ? TRACE_INDENT_ON : TRACE_DELIMITER_OFF;

        return printIndented(currentDepth, prefix + message);
    }

    private static String printIndented(int depth, String message) {
        if (depth < 0) {
            depth = 0;
        }
        String indent = " ".repeat(depth * 2);
        return indent + message;
    }
}
