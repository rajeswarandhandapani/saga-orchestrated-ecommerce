package com.rajeswaran.ecommerce.platform.messaging;

/** Kafka header names used by every service. */
public final class MessageHeaders {
    public static final String MESSAGE_ID = "messageId";
    public static final String MESSAGE_TYPE = "messageType";
    public static final String CORRELATION_ID = "correlationId";

    /** MDC key and HTTP header for the correlation id that follows a request through the whole saga. */
    public static final String CORRELATION_MDC_KEY = "correlationId";
    public static final String CORRELATION_HTTP_HEADER = "X-Correlation-Id";

    private MessageHeaders() {
    }
}
