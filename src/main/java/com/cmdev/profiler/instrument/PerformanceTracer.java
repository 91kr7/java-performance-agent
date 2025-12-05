package com.cmdev.profiler.instrument;

import net.bytebuddy.asm.Advice;

public class PerformanceTracer {

    @Advice.OnMethodEnter
    public static long onEnter(@Advice.Origin Class<?> clazz,
                               @Advice.Origin("#m") String methodName) {

        if (TimerContext.systemInstrumentationEnabled) {
            try {
                TraceInfos trace = new TraceInfos(clazz, methodName);
                TimerTracingUtils.trace(trace);
                return trace.getStartTime();
            } catch (Throwable e) {
                System.err.println("[CMDev] " + clazz.getSimpleName() + ": " + e.getMessage());
            }
        }
        return 0;
    }

    @Advice.OnMethodExit(onThrowable = Throwable.class)
    public static void onExit(@Advice.Origin Class<?> clazz,
                              @Advice.Origin("#m") String methodName,
                              @Advice.Enter long traceId) {

        if (TimerContext.systemInstrumentationEnabled) {
            try {
                TimerTracingUtils.trace(new TraceInfos(traceId, clazz, methodName, true));
            } catch (Throwable e) {
                System.err.println("[CMDev] " + clazz.getSimpleName() + ": " + e.getMessage());
            }
        }
    }
}
