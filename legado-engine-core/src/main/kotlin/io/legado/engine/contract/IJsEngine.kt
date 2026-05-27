package io.legado.engine.contract

/**
 * JavaScript execution abstraction.
 * The LNR host provides the JS engine binding.
 */
interface IJsEngine {
    /**
     * Evaluate a JS expression and return the result as a String.
     * @param script The JavaScript code to execute
     * @param bindings Variable bindings available in JS scope
     */
    suspend fun eval(script: String, bindings: Map<String, Any?> = emptyMap()): String?

    /**
     * Evaluate JS and return raw result (could be any type).
     */
    suspend fun evalAny(script: String, bindings: Map<String, Any?> = emptyMap()): Any?
}

