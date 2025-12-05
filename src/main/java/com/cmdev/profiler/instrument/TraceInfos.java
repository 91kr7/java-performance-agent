package com.cmdev.profiler.instrument;

public class TraceInfos {

    private String threadId;
    private Long traceInfoId;
    private long time;
    private Class<?> clazz;
    private String methodName;
    private boolean isEnd;
    private long deep;

    public TraceInfos(Class<?> clazz, String methodName) {
        this.time = System.nanoTime();
        this.clazz = clazz;
        this.methodName = methodName;
    }

    public TraceInfos(Long traceInfoId) {
        this.traceInfoId = traceInfoId;
        this.time = System.nanoTime();
        this.isEnd = true;
    }

    public void setThreadId(String threadId) {
        this.threadId = threadId;
    }

    public void setDeep(long deep) {
        this.deep = deep;
    }

    public String getThreadId() {
        return threadId;
    }

    public Class<?> getClazz() {
        return clazz;
    }

    public String getMethodName() {
        return methodName;
    }

    public boolean isEnd() {
        return isEnd;
    }

    public long getDeep() {
        return deep;
    }

    public long getTime() {
        return time;
    }

    public Long getTraceInfoId() {
        return traceInfoId;
    }

    public void setTraceInfoId(Long traceInfoId) {
        this.traceInfoId = traceInfoId;
    }
}
