package com.cmdev.profiler.instrument;

public class TraceMessage {

    private final String threadId;
    private final String prefix;
    private final String message;
    private final int depthOfTheMessage;
    private final boolean traceEnded;

    public TraceMessage(String threadId, String prefix, String message, int depthOfTheMessage, boolean traceEnded) {

        this.threadId = threadId;
        this.prefix = prefix;
        this.message = message;
        this.depthOfTheMessage = depthOfTheMessage;
        this.traceEnded = traceEnded;
    }

    public String getThreadId() {
        return threadId;
    }

    public String getPrefix() {
        return prefix;
    }

    public String getMessage() {
        return message;
    }

    public int getDepthOfTheMessage() {
        return depthOfTheMessage;
    }

    public boolean isTraceEnded() {
        return traceEnded;
    }
}
