package com.crosscheck.application.features.analysis.common.dto;

import java.util.List;

public record VerdictResult(String status, String explanation, String documentarySupport,
                            List<String> sourceIds) {
    public VerdictResult { sourceIds = List.copyOf(sourceIds); }
}
