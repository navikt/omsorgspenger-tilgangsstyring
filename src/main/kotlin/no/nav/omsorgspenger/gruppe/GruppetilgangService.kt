package no.nav.omsorgspenger.gruppe

import no.nav.omsorgspenger.auth.Token
import no.nav.omsorgspenger.api.Operasjon
import no.nav.omsorgspenger.secureLog

internal class GruppetilgangService(
    private val gruppeResolver: GruppeResolver) {

    internal fun kanGjøreOperasjon(
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
        Operasjon.Visning to setOf(
            Gruppe.Saksbehandler,
            Gruppe.Overstyrer,
            Gruppe.Beslutter,
            Gruppe.Oppgavestyrer,
            Gruppe.Veileder,
            Gruppe.Drift
        ),
        Operasjon.Endring to setOf(
            Gruppe.Saksbehandler,
            Gruppe.Overstyrer,
            Gruppe.Beslutter
        ),
        Operasjon.Unntakshåndtering to setOf(
            Gruppe.Drift
        )
    )
}