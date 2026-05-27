package io.legado.engine.exception

/**
 * Exception thrown during rule analysis.
 */
class RuleException(
    override val message: String,
    override val cause: Throwable? = null
) : Exception(message, cause)

/**
 * Exception thrown when a book source rule is invalid.
 */
class SourceException(
    override val message: String
) : Exception(message)

/**
 * Exception thrown when content fetching fails.
 */
class ContentException(
    override val message: String,
    override val cause: Throwable? = null
) : Exception(message, cause)
