package no.nav.omsorgspenger

import org.slf4j.Logger
import org.slf4j.LoggerFactory

private object SecureLogger {
    private const val LoggerName = "secureLogger"
    const val TomJson = "{}"
    const val EscapetTomJson = "\\$TomJson"
    val instance: Logger = LoggerFactory.getLogger(LoggerName)
}

internal fun String.sanitize() = replace(SecureLogger.TomJson, SecureLogger.EscapetTomJson)

internal fun secureLog(message: String) {
    SecureLogger.instance.info(message.sanitize())
}
