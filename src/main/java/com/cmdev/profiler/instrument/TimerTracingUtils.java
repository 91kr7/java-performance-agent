package com.cmdev.profiler.instrument;

import com.cmdev.profiler.instrument.daemon.TraceManagerDaemon;

import java.util.UUID;

public class TimerTracingUtils {

    private static final ThreadLocal<String> traceId = new ThreadLocal<>();
    private static final ThreadLocal<int[]> deepOfTheMessage = ThreadLocal.withInitial(() -> new int[1]);

    private TimerTracingUtils() {
    }

    private static String getThreadId() {
        return TimerContext.getTraceId();
    }

    public static void trace(TraceInfos traceInfos) {
        String threadIdLocal = traceId.get();
        if (threadIdLocal == null && TimerContext.methodToTrace.contains(traceInfos.getClazz().getName())) {
            threadIdLocal = UUID.randomUUID().toString();
            traceId.set(threadIdLocal);
        }

        if (threadIdLocal != null) {
            int[] depthHolder = deepOfTheMessage.get();
            int depthValue;
            if (!traceInfos.isEnd()) {
                depthValue = depthHolder[0]++;
            } else {
                depthValue = --depthHolder[0];
                if (depthValue < 0) {
                    depthValue = 0;
                    depthHolder[0] = 0;
                }
            }

            if (depthHolder[0] == 0) {
                traceId.remove();
                deepOfTheMessage.remove();
            }

            traceInfos.setThreadId(threadIdLocal);
            traceInfos.setDeep(depthValue);
            TraceManagerDaemon.putEntry(traceInfos);
        }
    }
}
