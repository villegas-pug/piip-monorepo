package pe.gob.midagri.piip.config;

import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/** Propaga una correlación segura o crea una nueva para toda respuesta HTTP. */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdFilter extends OncePerRequestFilter {

    public static final String REQUEST_ATTRIBUTE = CorrelationIdFilter.class.getName() + ".correlationId";
    private static final Pattern SAFE_CORRELATION_ID = Pattern.compile("[A-Za-z0-9-]{1,64}");

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String correlationId = validOrGenerated(request.getHeader(ApiHeaders.CORRELATION_ID));
        request.setAttribute(REQUEST_ATTRIBUTE, correlationId);
        response.setHeader(ApiHeaders.CORRELATION_ID, correlationId);
        MDC.put(ApiHeaders.CORRELATION_ID, correlationId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(ApiHeaders.CORRELATION_ID);
        }
    }

    public static String getCorrelationId(HttpServletRequest request) {
        Object value = request.getAttribute(REQUEST_ATTRIBUTE);
        return value instanceof String correlationId ? correlationId : "unknown";
    }

    private static String validOrGenerated(String candidate) {
        return candidate != null && SAFE_CORRELATION_ID.matcher(candidate).matches()
                ? candidate
                : UUID.randomUUID().toString();
    }
}
