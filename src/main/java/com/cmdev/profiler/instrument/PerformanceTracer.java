package com.cmdev.profiler.instrument;

import net.bytebuddy.asm.Advice;

public class PerformanceTracer {

    private static final String METHOD_SEPARATOR = ":";
    private static final String ID_SEPARATOR = "|";
    private static final String TIME_SEPARATOR = ">";

    @Advice.OnMethodEnter
    public static long onEnter(@Advice.Origin Class<?> clazz,
                               @Advice.Origin("#m") String methodName) {
        if (TimerContext.systemInstrumentationEnabled) {
            long startTime = System.nanoTime();
            try {
                String trace = startTime + ID_SEPARATOR + clazz.getName() + METHOD_SEPARATOR + methodName;
                TimerTracingUtils.tryToTrace(trace, true);
            } catch (Throwable e) {
                System.err.println("[CMDev] " + clazz.getSimpleName() + ": " + e.getMessage());
            }
            return startTime;
        }
        return -1;
    }

    @Advice.OnMethodExit(onThrowable = Throwable.class)
    public static void onExit(@Advice.Origin Class<?> clazz,
                              @Advice.Origin("#m") String methodName,
                              @Advice.Enter long startTime) {
        if (TimerContext.systemInstrumentationEnabled) {
            long durationNs = System.nanoTime() - startTime;
            try {
                String trace = startTime + ID_SEPARATOR + clazz.getName() + METHOD_SEPARATOR + methodName + TIME_SEPARATOR + durationNs;
                TimerTracingUtils.tryToTrace(trace, false);
            } catch (Throwable e) {
                System.err.println("[CMDev] " + clazz.getSimpleName() + ": " + e.getMessage());
            }
        }
    }
}
