package com.mcp.client.filter;

import io.github.bucket4j.*;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.stereotype.Component;
import java.io.IOException;
import java.time.Duration;

@Component
public class RateLimitFilter implements Filter {
    private final Bucket bucket = Bucket4j.builder()
            .addLimit(Bandwidth.simple(50, Duration.ofMinutes(1)))
            .build();

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        if (bucket.tryConsume(1)) {
            chain.doFilter(request, response);
        } else {
            ((HttpServletResponse) response).setStatus(429);
            response.getWriter().write("Rate limit exceeded");
        }
    }
}
