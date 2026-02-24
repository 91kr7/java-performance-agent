package com.cmdev.profiler.instrument.daemon;

import com.cmdev.profiler.instrument.TraceInfos;
import com.cmdev.profiler.instrument.TracingGlobalStatus;
import com.cmdev.profiler.instrument.io.PerformanceFileWriter;
import org.jctools.queues.MpscArrayQueue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TraceManagerDaemon extends Thread {

    private static final String ID_SEPARATOR = "@";
    private static final String METHOD_SEPARATOR = ":";
    private static final String DOT = ".";
    private static final String TIME_SEPARATOR = "|";
    private static final String TRACE_INDENT_ON = "+";
    private static final String TRACE_DELIMITER_OFF = "-";
    private static final String OUTPUTDIR = "/tmp/traces/";
    private static final MpscArrayQueue<TraceInfos> traceQueue = new MpscArrayQueue<>(131_072); // capacità
    private static final Map<String, PerformanceFileWriter> outputBuffer = new ConcurrentHashMap<>();

    public static void putEntry(TraceInfos trace) {
        traceQueue.offer(trace);
    }

    @Override
    public void run() {

        try {
            Files.createDirectory(Paths.get(OUTPUTDIR));
            System.out.println("[CMDev] Trace dir created!");
        } catch (IOException e) {
            System.out.println("[CMDev] Trace dir already exists!");
        }
        while (true) {
            TraceInfos trace = traceQueue.poll();
            if (trace != null) {
                processEntry(trace);
            } else {
                try {
                    Thread.sleep(getSleepTime());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
    }

    private int getSleepTime() {
        return TracingGlobalStatus.systemInstrumentationEnabled ? 5 : 1000;
    }

    private void processEntry(TraceInfos trace) {

        try {
            if (!(TracingGlobalStatus.packageToExclude != null && TracingGlobalStatus.packageToExclude.contains(trace.getClazz().getPackage().getName()))) {
                String logTrace;
                if (!trace.isEnd()) {
                    logTrace = trace.getDeep() + TRACE_INDENT_ON + ID_SEPARATOR + trace.getTraceInfoId() + TIME_SEPARATOR + trace.getTime() + METHOD_SEPARATOR + trace.getClazz().getName() + DOT + trace.getMethodName();
                } else {
                    logTrace = trace.getDeep() + TRACE_DELIMITER_OFF + ID_SEPARATOR + trace.getTraceInfoId() + TIME_SEPARATOR + trace.getTime();
                }
                PerformanceFileWriter writer = outputBuffer.computeIfAbsent(trace.getThreadId(), id -> new PerformanceFileWriter(OUTPUTDIR + trace.getThreadId()));

                writer.writeLine(logTrace);
                if (trace.isEnd()) {
                    writer.flush();
                    if (trace.getDeep() == 0) {
                        writer.close();
                        outputBuffer.remove(trace.getThreadId());
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("[CMDev] Error while processing trace message " + trace.getDeep() + ": " + e.getMessage());
        }
    }
}
