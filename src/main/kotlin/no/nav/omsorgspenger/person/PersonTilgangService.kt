package no.nav.omsorgspenger.person

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import no.nav.omsorgspenger.auth.Token
import no.nav.omsorgspenger.pdl.BadRequest
import no.nav.omsorgspenger.pdl.NotFound
import no.nav.omsorgspenger.pdl.PdlClient
import no.nav.omsorgspenger.pdl.ServerError
import no.nav.omsorgspenger.pdl.Unauthenticated
import no.nav.omsorgspenger.pdl.Unauthorised
import org.slf4j.LoggerFactory
import java.time.Duration

internal class PersonTilgangService(
    private val pdlClient: PdlClient) {
    private val logger = LoggerFactory.getLogger(PersonTilgangService::class.java)
    private val cache = personOppslagCache()

    internal suspend fun sjekkTilgang(
        identitetsnummer: Set<String>,
        correlationId: String,
        token: Token): Boolean {
        val cacheKey = CacheKey(
            username = token.username,
            identitetsnummer = identitetsnummer
        )
        return cache.getIfPresent(cacheKey) ?: sjekkTilgangMotPdl(
            identitetsnummer = identitetsnummer,
            correlationId = correlationId,
            token = token
        ).also { cache.put(cacheKey, it) }
    }

    private suspend fun sjekkTilgangMotPdl(
        identitetsnummer: Set<String>,
        correlationId: String,
        token: Token): Boolean {

        val errors = pdlClient.hentInfoOmPersoner(
            identitetsnummer = identitetsnummer,
            correlationId = correlationId,
            token = token).flatMap {
            it.errors ?: emptyList()
        }

        when (errors.size) {
            0 -> Unit
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

    internal companion object {
        internal data class CacheKey(
            private val username: String,
            private val identitetsnummer: Set<String>
        )
        internal fun personOppslagCache() : Cache<CacheKey, Boolean> = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofMinutes(15))
            .maximumSize(200)
            .build()
    }
}
