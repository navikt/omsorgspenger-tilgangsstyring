package no.nav.omsorgspenger.person

import no.nav.omsorgspenger.pdl.PdlClient

internal class PersonTilgangService(
    private val pdlClient: PdlClient
) {

    internal suspend fun sjekkTilgang(identitetsnummer: List<String>, correlationId: String, jwt: String): Boolean {
        val (data, errors) = pdlClient.hentInfoOmPersoner(identitetsnummer.toSet(), correlationId, jwt)
        return true
    }
}
