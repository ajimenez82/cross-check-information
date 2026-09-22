package com.crosscheck.presentation.api.error;

public record ApiErrorResponse(String code, String message, String requestId, String executionState) {}
