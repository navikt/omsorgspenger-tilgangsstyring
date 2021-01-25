package no.nav.omsorgspenger.auth

import no.nav.omsorgspenger.person.Operasjon
import no.nav.omsorgspenger.secureLog

internal class GruppetilgangService(
    private val gruppeResolver: GruppeResolver) {

    internal suspend fun kanGjøreOperasjon(
        operasjon: Operasjon,
        token: Token,
        correlationId: String) : Boolean {
        val grupper = gruppeResolver.resolve(token, correlationId)
        return måVæreMedlemAvEnAv
            .getValue(operasjon)
            .intersect(grupper)
            .isNotEmpty()
            .also { harTilgang -> if (!harTilgang) {
                secureLog("Er ikke medlem av rett gruppe for $operasjon. Er medlem av $grupper")
            }}

    }

    private val måVæreMedlemAvEnAv = mapOf(
        Operasjon.Visning to Gruppe.values().toSet()
    )
}