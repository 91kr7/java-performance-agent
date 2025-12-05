package com.cmdev.profiler.instrument;

import net.bytebuddy.asm.Advice;

public class PerformanceTracer {

    @Advice.OnMethodEnter
    public static void onEnter(@Advice.Origin Class<?> clazz,
                               @Advice.Origin("#m") String methodName) {

        if (TimerContext.systemInstrumentationEnabled) {
            try {
                TimerTracingUtils.trace(new TraceInfos(clazz, methodName));
            } catch (Throwable e) {
                System.err.println("[CMDev] " + clazz.getSimpleName() + ": " + e.getMessage());
            }
        }
    }

    @Advice.OnMethodExit(onThrowable = Throwable.class)
    public static void onExit(@Advice.Origin Class<?> clazz,
                              @Advice.Origin("#m") String methodName) {

        if (TimerContext.systemInstrumentationEnabled) {
            try {
                TimerTracingUtils.trace(new TraceInfos(clazz, methodName, true));
            } catch (Throwable e) {
                System.err.println("[CMDev] " + clazz.getSimpleName() + ": " + e.getMessage());
            }
        }
    }
}
