package com.cmdev.profiler.instrument;

import com.cmdev.profiler.instrument.daemon.TraceManagerDaemon;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class TimerTracingUtils {

    private static final String TRACE_INDENT_ON = "+ ";
    private static final String TRACE_DELIMITER_OFF = "- ";
    private static final Map<String, AtomicInteger> callDeep = new ConcurrentHashMap<>();

    private TimerTracingUtils() {

    }

    private static String getThreadId() {
        return TimerContext.getTraceId();
    }

    public static void tryToTrace(String message, boolean isMethodStart) {
        try {
            if (TimerContext.systemInstrumentationEnabled
                    && (TimerContext.isTraceEnabled() || message.contains(TimerContext.methodToTrace))
                    && !containsExcludedPackage(message, TimerContext.packageToExclude)) {
                if (!TimerContext.isTraceEnabled()) {
                    TimerContext.initTrace(message);
                }
                trace(message, isMethodStart);
            }

        } catch (Exception e) {
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

    public static void trace(String message, boolean isMethodStart) {

        String threadId = getThreadId();
        if (threadId != null) {

            AtomicInteger depth = callDeep.computeIfAbsent(threadId, id -> new AtomicInteger(0));

            int depthOfTheMessage;
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
            if (traceEnded) {
                TimerContext.stopTrace();
                callDeep.remove(threadId);
            }
            TraceManagerDaemon.putEntry(
                    new TraceMessage(threadId,
                            isMethodStart ? TRACE_INDENT_ON : TRACE_DELIMITER_OFF,
                            message,
                            depthOfTheMessage,
                            traceEnded));
        }
    }
}
