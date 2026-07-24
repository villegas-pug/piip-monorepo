package pe.gob.midagri.piip.config;

/** Nombres canónicos de headers HTTP transversales de PIIP. */
public final class ApiHeaders {

    public static final String CORRELATION_ID = "X-Correlation-Id";
    public static final String EFFECTIVE_ASSIGNMENT_ID = "X-Asignacion-Efectiva-Id";
    public static final String IDEMPOTENCY_KEY = "Idempotency-Key";
    public static final String IF_MATCH = "If-Match";

    private ApiHeaders() {
    }
}
