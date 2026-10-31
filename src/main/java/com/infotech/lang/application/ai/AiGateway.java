package com.infotech.lang.application.ai;

/**
 * The only way the application talks to a model. Everything that needs a model goes through this interface so it can
 * be swapped (Muse Spark, Muse Spark Open) or faked in tests.
 */
public interface AiGateway {

    /** Sends one request that must come back as JSON matching the schema. Never throws; failures come back in the result. */
    AiResult completeJson(AiRequest request);
}
