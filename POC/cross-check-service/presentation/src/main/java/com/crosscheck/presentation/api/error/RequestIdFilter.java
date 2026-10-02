package com.crosscheck.presentation.api.error;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.springframework.web.filter.OncePerRequestFilter;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;

/** Generates diagnostic identifiers without trusting or logging client-provided values. */
@Slf4j
public class RequestIdFilter extends OncePerRequestFilter {
    public static final String ATTRIBUTE = RequestIdFilter.class.getName() + ".requestId";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                  FilterChain chain) throws ServletException, IOException {
        String requestId = UUID.randomUUID().toString();
        request.setAttribute(ATTRIBUTE, requestId);
        response.setHeader("X-Request-Id", requestId);
        if (request.getRequestURI().startsWith(request.getContextPath() + "/api/")) {
            response.setHeader("Cache-Control", "no-store");
        }
        boolean analysis = request.getRequestURI().equals(request.getContextPath() + "/api/analysis/start");
        long started = System.nanoTime();
        String previousId = MDC.get("requestId");
        MDC.put("requestId", requestId);
        boolean completed = false;
        try {
            if (analysis) log.info("Analysis request received: requestId={}", requestId);
            chain.doFilter(request, response);
            completed = true;
        } finally {
            if (analysis) log.info("Analysis request finished: requestId={}, status={}, completed={}, elapsedMs={}",
                    requestId, response.getStatus(), completed, (System.nanoTime() - started) / 1_000_000);
            if (previousId == null) MDC.remove("requestId"); else MDC.put("requestId", previousId);
        }
    }
}
