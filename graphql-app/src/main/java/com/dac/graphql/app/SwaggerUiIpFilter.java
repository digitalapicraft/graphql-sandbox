package com.dac.graphql.app;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class SwaggerUiIpFilter implements Filter {
    @Value("${app.swagger-ui.allowed-urls:/swagger-ui/}")
    private String allowedUrlsConfig;

    @Value("${app.swagger-ui.allowed-ips:}")
    private String allowedIpsConfig;

    private List<String> getAllowedUrls() {
        return Arrays.stream(allowedUrlsConfig.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }

    private List<String> getAllowedIps() {
        return Arrays.stream(allowedIpsConfig.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;
        String uri = req.getRequestURI();
        String ip = req.getRemoteAddr();
        List<String> allowedUrls = getAllowedUrls();
        List<String> allowedIps = getAllowedIps();
        boolean isSwaggerUiRequest = allowedUrls.stream().anyMatch(uri::startsWith);
        boolean ipAllowed = allowedIps.isEmpty() || allowedIps.contains(ip);
        if (isSwaggerUiRequest && !ipAllowed) {
            res.sendError(HttpServletResponse.SC_FORBIDDEN, "Forbidden: Swagger UI is not accessible from your IP address");
            return;
        }
        chain.doFilter(request, response);
    }
} 