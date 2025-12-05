package com.cmdev.profiler.instrument;

import net.bytebuddy.asm.Advice;

public class PerformanceTracer {

    @Advice.OnMethodEnter
    public static Long onEnter(@Advice.Origin Class<?> clazz,
                               @Advice.Origin("#m") String methodName) {

        if (TimerContext.systemInstrumentationEnabled) {
            try {
                TraceInfos trace = new TraceInfos(clazz, methodName);
                TimerTracingUtils.trace(trace);
                return trace.getTraceInfoId();
            } catch (Throwable e) {
                System.err.println("[CMDev] " + clazz.getSimpleName() + ": " + e.getMessage());
            }
        }
        return null;
    }

    @Advice.OnMethodExit(onThrowable = Throwable.class)
    public static void onExit(@Advice.Origin Class<?> clazz,
                              @Advice.Origin("#m") String methodName,
                              @Advice.Enter Long traceInfoId) {

        if (TimerContext.systemInstrumentationEnabled) {
            try {
                TimerTracingUtils.trace(new TraceInfos(traceInfoId));
            } catch (Throwable e) {
                System.err.println("[CMDev] " + clazz.getSimpleName() + ": " + e.getMessage());
            }
        }
    }
}
