package io.github.dmzz_yyhyy.lnrplugin.util

import io.legado.engine.contract.IJsEngine
import org.mozilla.javascript.Context
import org.mozilla.javascript.ScriptableObject

/**
 * JavaScript engine implementation using Mozilla Rhino.
 * Provides JS execution capability for book source rules.
 */
class JsEngineImpl : IJsEngine {

    override suspend fun eval(script: String, bindings: Map<String, Any?>): String? {
        return try {
            val cx = Context.enter()
            try {
                cx.optimizationLevel = -1 // Required for Android
                val scope = cx.initStandardObjects()

                // Inject bindings into JS scope
                bindings.forEach { (key, value) ->
                    ScriptableObject.putProperty(scope, key, Context.javaToJS(value, scope))
                }

                val result = cx.evaluateString(scope, script, "rule.js", 1, null)
                result?.toString()
            } finally {
                Context.exit()
            }
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun evalAny(script: String, bindings: Map<String, Any?>): Any? {
        return try {
            val cx = Context.enter()
            try {
                cx.optimizationLevel = -1
                val scope = cx.initStandardObjects()
                bindings.forEach { (key, value) ->
                    ScriptableObject.putProperty(scope, key, Context.javaToJS(value, scope))
                }
                val result = cx.evaluateString(scope, script, "rule.js", 1, null)
                when (result) {
                    is org.mozilla.javascript.Undefined -> null
                    else -> unwrapRhinoResult(result)
                }
            } finally {
                Context.exit()
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun unwrapRhinoResult(result: Any?): Any? {
        return when (result) {
            null -> null
            is org.mozilla.javascript.Undefined -> null
            is org.mozilla.javascript.NativeArray -> {
                (0 until result.length).map { unwrapRhinoResult(result.get(it.toInt(), result)) }
            }
            is org.mozilla.javascript.NativeObject -> {
                val map = mutableMapOf<String, Any?>()
                result.ids.forEach { id ->
                    if (id is String) {
                        map[id] = unwrapRhinoResult(result.get(id, result))
                    }
                }
                map
            }
            is Double -> if (result == result.toLong().toDouble()) result.toLong() else result
            else -> result.toString()
        }
    }
}
