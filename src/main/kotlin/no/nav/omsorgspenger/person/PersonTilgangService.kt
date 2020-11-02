package no.nav.omsorgspenger.person

import no.nav.omsorgspenger.pdl.BadRequest
import no.nav.omsorgspenger.pdl.NotFound
import no.nav.omsorgspenger.pdl.PdlClient
import no.nav.omsorgspenger.pdl.ServerError
import no.nav.omsorgspenger.pdl.Unauthenticated
import no.nav.omsorgspenger.pdl.Unauthorised
import org.slf4j.LoggerFactory

internal class PersonTilgangService(
    private val pdlClient: PdlClient
) {

    private val logger = LoggerFactory.getLogger(PersonTilgangService::class.java)

    internal suspend fun sjekkTilgang(identitetsnummer: List<String>, correlationId: String, authHeader: String): Boolean {
        val errors = pdlClient.hentInfoOmPersoner(identitetsnummer.toSet(), correlationId, authHeader).errors
        when (errors?.size) {
            null, 0 -> Unit
            else -> {
                logger.info("Feil fra PDL: $errors")
                val errorCodes = errors.map { it.extensions.code }
                if (errorCodes.any { it == Unauthenticated || it == Unauthorised }) {
                    return false
                }
                if (errorCodes.any { it == BadRequest || it == ServerError }) {
                    throw RuntimeException("Bad request eller server error fra PDL")
                }
                val ukjenteFeil = errorCodes.filter { it != NotFound }
                if (ukjenteFeil.isNotEmpty()) {
                    throw RuntimeException("Ukjent(e) feil fra PDL")
                }
            }
        }

        return true
    }
}
