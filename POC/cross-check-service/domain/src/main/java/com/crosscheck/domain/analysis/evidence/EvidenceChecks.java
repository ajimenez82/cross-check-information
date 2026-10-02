package com.crosscheck.domain.analysis.evidence;

import java.net.URI;
import java.util.List;
import java.util.Objects;

final class EvidenceChecks {
    private EvidenceChecks() {}

    static void require(boolean condition, String message) {
        if (!condition) throw new IllegalArgumentException(message);
    }

    static String text(String value, String field) {
        require(value != null && !value.isBlank(), field + " is required");
        return value;
    }

    static <T> T required(T value, String field) {
        require(value != null, field + " is required");
        return value;
    }

    static <T> List<T> list(List<T> value, String field) {
        required(value, field);
        require(value.stream().noneMatch(Objects::isNull), field + " contains null");
        return List.copyOf(value);
    }

    static URI url(URI value) {
        required(value, "url");
        require(("https".equalsIgnoreCase(value.getScheme()) || "http".equalsIgnoreCase(value.getScheme()))
                && value.getHost() != null && value.getUserInfo() == null, "Invalid source URL");
        return value;
    }
}
