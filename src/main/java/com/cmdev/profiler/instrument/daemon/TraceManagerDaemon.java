package com.cmdev.profiler.instrument.daemon;

import com.cmdev.profiler.instrument.TraceInfos;
import com.cmdev.profiler.instrument.io.PerformanceFileWriter;
import org.jctools.queues.MpscArrayQueue;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TraceManagerDaemon extends Thread {

    private static final String METHOD_SEPARATOR = ":";
    private static final String ID_SEPARATOR = "|";
    private static final String END_SEPARATOR = ">";
    private static final String TRACE_INDENT_ON = "+ ";
    private static final String TRACE_DELIMITER_OFF = "- ";
    private static final String OUTPUTDIR = "/tmp/traces/";
    private static final MpscArrayQueue<TraceInfos> traceQueue = new MpscArrayQueue<>(131_072); // capacità
    private static final Map<String, PerformanceFileWriter> outputBuffer = new ConcurrentHashMap<>();

    public static void putEntry(TraceInfos trace) {
        traceQueue.offer(trace);
    }

    private static String intendMessage(int depth, String message) {
        if (depth < 0) {
            depth = 0;
        }
        String indent = " ".repeat(depth * 2);
        return indent + message;
    }

    @Override
    public void run() {

        while (true) {
            TraceInfos trace = traceQueue.poll();
            if (trace != null) {
                processEntry(trace);
            } else {
                try {
                    Thread.sleep(100); // Reduced sleep time for better responsiveness
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
    }

    private void processEntry(TraceInfos trace) {

        try {
            String logTrace;

            // Be Carefull! the startTime of the trace is also the trace ID
            if (trace.isEnd()) {
                logTrace = TRACE_DELIMITER_OFF + trace.getStartTime() + END_SEPARATOR + trace.getEndTime();
            } else {
                logTrace = TRACE_INDENT_ON + trace.getStartTime() + ID_SEPARATOR + trace.getClazz().getName() + METHOD_SEPARATOR + trace.getMethodName();
            }
            PerformanceFileWriter writer = outputBuffer.computeIfAbsent(trace.getThreadId(), id -> new PerformanceFileWriter(OUTPUTDIR + trace.getThreadId()));

            writer.writeLine(intendMessage(trace.getDeep(), logTrace));
            if (trace.isEnd()) {
                writer.close();
                outputBuffer.remove(trace.getThreadId());
            }
        } catch (Exception e) {
            System.err.println("[CMDev] Error while processing trace message " + trace.getDeep() + ": " + e.getMessage());
        }
    }
}
