package no.nav.omsorgspenger

import io.ktor.auth.jwt.*
import no.nav.omsorgspenger.person.Operasjon
import org.json.JSONObject
import org.slf4j.LoggerFactory

internal class Gruppetilgang(grupperResourcePath: String) {
    private val gruppeMapping = grupperResourcePath.fraResources()

    init {
        logger.info("Gruppemapping")
        gruppeMapping.forEach { (gruppe, id) -> logger.info("Gruppe[$gruppe]=$id") }
    }

    internal fun kanGjøreOperasjon(
        jwt: JWTPrincipal,
        operasjon: Operasjon) : Boolean {

        if (operasjon != Operasjon.Visning) {
            return logger.warn("Støtter ikke operasjon $operasjon").let { false }
        }

        val grupper = kotlin.runCatching {
            jwt.payload.getClaim("groups").asArray(String::class.java)
        }.fold(
            onSuccess = { it },
            onFailure = { arrayOf() }
        ).toSet()


        val harTilgang= grupper.contains(gruppeMapping[Gruppe.Saksbehandler]) || grupper.contains(gruppeMapping[Gruppe.Veileder])
        if (!harTilgang) {
            secureLog("Er ikke medlem av rett gruppe for $operasjon. Er medlem av $grupper")
        }
        return harTilgang
    }

    private enum class Gruppe {
        Saksbehandler,
        Veileder
    }

    private fun String.fraResources() = requireNotNull(Thread.currentThread().contextClassLoader.getResource(this)) {
        "Finner ikke gruppeconfig på resource path '$this'"
    }.readText(charset = Charsets.UTF_8)
        .let { JSONObject(it) }.toMap()
        .mapValues { it.value.toString() }
        .mapKeys { Gruppe.valueOf(it.key) }
        .also { require(it.keys.containsAll(Gruppe.values().toList())) }

    private companion object {
        private val logger = LoggerFactory.getLogger(Gruppetilgang::class.java)
    }
}