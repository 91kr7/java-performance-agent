package com.cmdev.profiler.instrument;

import java.util.UUID;

public class TimerContext {

    public static boolean systemInstrumentationEnabled = false;
    public static String methodToTrace = null;
    public static String[] packageToExclude = null;

    private static final ThreadLocal<Boolean> traceEnabled = ThreadLocal.withInitial(() -> false);
    private static final ThreadLocal<String> traceId = new ThreadLocal<>();

    public static void initTrace(String message) {
        traceEnabled.set(true);
        traceId.set(message + "-" + UUID.randomUUID());
    }

    public static boolean isTraceEnabled() {
        return traceEnabled.get();
    }

    public static void stopTrace() {
        traceEnabled.remove();
        traceId.remove();
    }

    public static String getTraceId() {
        return traceId.get();
    }
}
