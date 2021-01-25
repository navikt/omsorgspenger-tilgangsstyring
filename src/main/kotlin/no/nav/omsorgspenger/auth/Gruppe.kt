package no.nav.omsorgspenger.auth

import org.json.JSONObject
import org.slf4j.LoggerFactory

internal class GruppeResolver(
    azureGroupMappingPath: String) {
    private val azureGroupMapping = azureGroupMappingPath.groupMappingFromResources()

    init {
        logger.info("Azure Gruppemapping")
        azureGroupMapping.forEach { (gruppe, id) -> logger.info("Gruppe[$gruppe]=$id") }
    }

    internal suspend fun resolve(token: Token, correlationId: String) = when (token) {
        is AzureToken -> token.resolve()
        else -> (token as OpenAmToken).resolve(correlationId)
    }

    private fun AzureToken.resolve() = azureGroupMapping.filterValues {
        it in jwt.claims.getValue("groups").asArray(String::class.java)
    }.keys

    private suspend fun OpenAmToken.resolve(correlationId: String) = setOf<Gruppe>() // TODO

    private companion object {
        private val logger = LoggerFactory.getLogger(GruppeResolver::class.java)
        private fun String.groupMappingFromResources() = requireNotNull(Thread.currentThread().contextClassLoader.getResource(this)) {
            "Finner ikke gruppemapping på resource path '$this'"
        }.readText(charset = Charsets.UTF_8)
            .let { JSONObject(it) }.toMap()
            .mapValues { it.value.toString() }
            .mapKeys { Gruppe.valueOf(it.key) }
            .also { require(it.keys.containsAll(Gruppe.values().toList())) }
    }
}

enum class Gruppe {
    Saksbehandler,
    Veileder,
    Beslutter,
    Oppgavestyrer,
    Overstyrer
    //Drift TODO
}