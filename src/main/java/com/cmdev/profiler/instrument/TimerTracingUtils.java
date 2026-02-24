package com.cmdev.profiler.instrument;

import com.cmdev.profiler.instrument.daemon.TraceManagerDaemon;

import java.util.UUID;

public class TimerTracingUtils {

    private static final ThreadLocal<String> traceId = new ThreadLocal<>(); // ID Of the all trace file
    private static final ThreadLocal<long[]> traceInfoId = ThreadLocal.withInitial(() -> new long[1]); // ID of The single line of tracing
    private static final ThreadLocal<long[]> deepOfTheMessage = ThreadLocal.withInitial(() -> new long[1]);

    private TimerTracingUtils() {
    }

    public static void trace(TraceInfos traceInfos) {
        String threadIdLocal = traceId.get();
        if (threadIdLocal == null && traceInfos.getClazz() != null && TracingGlobalStatus.methodToTrace.contains(traceInfos.getClazz().getName())) {
            threadIdLocal = UUID.randomUUID().toString();
            traceId.set(threadIdLocal);
        }
        if (threadIdLocal != null) {
            if (traceInfos.getTraceInfoId() == null) {
                traceInfos.setTraceInfoId(traceInfoId.get()[0]++);
            }
            long[] depthHolder = deepOfTheMessage.get();
            long depthValue;
            if (!traceInfos.isEnd()) {
                depthValue = depthHolder[0]++;
            } else {
                depthValue = --depthHolder[0];
                if (depthValue < 0) {
                    depthValue = 0;
                    depthHolder[0] = 0;
                }
            }

            if (traceInfos.isEnd() && depthHolder[0] == 0) {
                traceId.remove();
                traceInfoId.remove();
                deepOfTheMessage.remove();
            }

            traceInfos.setThreadId(threadIdLocal);
            traceInfos.setDeep(depthValue);
            TraceManagerDaemon.putEntry(traceInfos);
        }
    }
}
