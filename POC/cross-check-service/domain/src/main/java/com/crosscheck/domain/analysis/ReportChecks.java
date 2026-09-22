package com.crosscheck.domain.analysis;

import java.net.URI;
import java.util.HashSet;
import java.util.List;

final class ReportChecks {
    private ReportChecks() {}

    static void require(boolean condition, String message) {
        if (!condition) {
            throw new InvalidAnalysisReportException(message);
        }
    }

    static String text(String value, String field) {
        require(value != null && !value.isBlank(), field + " es obligatorio");
        return value;
    }

    static <T> T required(T value, String field) {
        require(value != null, field + " es obligatorio");
        return value;
    }

    static String optionalText(String value, String field) {
        if (value != null) {
            text(value, field);
        }
        return value;
    }

    static <T> List<T> list(List<T> values, String field) {
        required(values, field);
        require(values.stream().noneMatch(java.util.Objects::isNull), field + " contiene null");
        return List.copyOf(values);
    }

    static List<String> texts(List<String> values, String field) {
        var copy = list(values, field);
        copy.forEach(value -> text(value, field));
        return copy;
    }

    static List<String> ids(List<String> values, String field) {
        var copy = texts(values, field);
        require(new HashSet<>(copy).size() == copy.size(), field + " contiene duplicados");
        return copy;
    }

    static URI url(URI value) {
        required(value, "url");
        require(("https".equalsIgnoreCase(value.getScheme()) || "http".equalsIgnoreCase(value.getScheme()))
                && value.getHost() != null && value.getUserInfo() == null,
                "url debe ser HTTP(S), absoluta y sin credenciales");
        return value;
    }
}
