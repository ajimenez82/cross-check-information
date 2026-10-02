package com.crosscheck.application.features.analysis.jobs;
public final class JobException extends RuntimeException {
    private final int status;
    private final String code;
    public JobException(int status, String code) { super(code); this.status = status; this.code = code; }
    public int status() { return status; }
    public String code() { return code; }
}
