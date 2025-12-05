package com.cmdev.profiler.instrument;

public class TraceInfos {

    private String threadId;
    private long startTime;
    private Class<?> clazz;
    private String methodName;
    private boolean isEnd;
    private long deep;

    public TraceInfos(Class<?> clazz, String methodName) {
        this.startTime = System.nanoTime();
        this.clazz = clazz;
        this.methodName = methodName;
    }

    public TraceInfos(Class<?> clazz, String methodName, boolean isEnd) {
        this.startTime = System.nanoTime();
        this.clazz = clazz;
        this.methodName = methodName;
        this.isEnd = isEnd;
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

    public long getStartTime() {
        return startTime;
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
}
