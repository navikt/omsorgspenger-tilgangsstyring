package no.nav.omsorgspenger.api

import com.fasterxml.jackson.databind.exc.MismatchedInputException
import com.fasterxml.jackson.module.kotlin.MissingKotlinParameterException
import io.ktor.application.*
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.request.*
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.routing.post
import no.nav.omsorgspenger.gruppe.GruppetilgangService
import no.nav.omsorgspenger.auth.TokenResolver
import no.nav.omsorgspenger.person.PersonTilgangService
import no.nav.omsorgspenger.secureLog
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("no.nav.omsorgspenger.api.TilgangApi")

internal fun Route.TilgangApi(
    tokenResolver: TokenResolver,
    personTilgangService: PersonTilgangService,
    gruppetilgangService: GruppetilgangService) {

    fun Pair<ApplicationCall, Set<String>>.sjekkTilgangTilPersoner() : Boolean {
        val sjekkTilgangTilpersoner = first.request.path() == "/api/tilgang/personer"
        if (sjekkTilgangTilpersoner) { require(second.isNotEmpty()) {
            "Må sendes med minst et identitetsnummer når det gjøres tilgangssjekk på personer."
        }} else { require(second.isEmpty()) {
            "Kan ikke sendes med noen identitetsnummer når det gjøres tilgangssjekk kun på operasjon."
        }}
        return sjekkTilgangTilpersoner
    }

    post("/api/tilgang{...}") {
        val (_, token) = tokenResolver.resolve(call) ?: return@post call.respond(HttpStatusCode.Unauthorized)
        val correlationId = call.request.headers[HttpHeaders.XCorrelationId] ?: return@post call.respond(HttpStatusCode.BadRequest)

        if (!token.erPersonToken) {
            logger.warn("Tilgangsstyring ble kalt fra et system og ikke en personbruker. Issuer: ${token.jwt.issuer}, ClientId: ${token.clientId}")
            return@post call.respond(HttpStatusCode.Forbidden)
        }

        val (identitetsnummer, operasjon, beskrivelse) = kotlin.runCatching {
            call.receive<TilgangRequest>()
        }.fold(
            onSuccess = { requestBody -> requestBody },
            onFailure = { ex ->
                when (ex) {
                    is MismatchedInputException,
                    is MissingKotlinParameterException -> {
                        logger.error("Mapping exception", ex)
                        return@post call.respond(HttpStatusCode.BadRequest, ex.localizedMessage)
                    }
                    else -> throw ex
                }
            }
        )

        val sjekkTilgangTilPersoner = (call to identitetsnummer).sjekkTilgangTilPersoner()

        val secureLogMessage = if (sjekkTilgangTilPersoner) {
            "Personen ${token.username} ønsker å $beskrivelse ($operasjon) for identitetsnummer $identitetsnummer"
        } else {
            "Personen ${token.username} ønsker å $beskrivelse ($operasjon)"
        }

        if (!gruppetilgangService.kanGjøreOperasjon(operasjon = operasjon, token = token, correlationId = correlationId)) {
            secureLog("Avslått: $secureLogMessage")
            return@post call.respond(HttpStatusCode.Forbidden)
        }

        if (!sjekkTilgangTilPersoner) {
            secureLog("Innvilget: $secureLogMessage")
            return@post call.respond(HttpStatusCode.NoContent)
        }

        when (personTilgangService.sjekkTilgang(
            identitetsnummer = identitetsnummer,
            correlationId = correlationId,
            token = token)) {
            true -> {
                secureLog("Innvilget: $secureLogMessage")
                call.respond(HttpStatusCode.NoContent)
            }
            false -> {
                secureLog("Avslått: $secureLogMessage")
                call.respond(HttpStatusCode.Forbidden)
            }
        }
    }
}
