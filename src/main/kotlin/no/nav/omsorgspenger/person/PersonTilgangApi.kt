package no.nav.omsorgspenger.person

import com.fasterxml.jackson.databind.exc.MismatchedInputException
import com.fasterxml.jackson.module.kotlin.MissingKotlinParameterException
import io.ktor.application.call
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.routing.post
import no.nav.omsorgspenger.auth.GruppetilgangService
import no.nav.omsorgspenger.auth.TokenResolver
import no.nav.omsorgspenger.secureLog
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("no.nav.omsorgspenger.person.PersonTilgangApi")

internal fun Route.PersonTilgangApi(
    tokenResolver: TokenResolver,
    personTilgangService: PersonTilgangService,
    gruppetilgangService: GruppetilgangService) {

    post("/api/tilgang/personer") {
        val (authorizationHeader, token) = tokenResolver.resolve(call) ?: return@post call.respond(HttpStatusCode.Unauthorized)

        if (!token.erPersonToken) {
            logger.warn("Tilgangsstyring ble kalt fra et system og ikke en personbruker. Issuer: ${token.jwt.issuer}, ClientId: ${token.clientId}")
            return@post call.respond(HttpStatusCode.Forbidden)
        }

        val (identitetsnummer, operasjon, beskrivelse) = kotlin.runCatching {
            call.receive<PersonerRequestBody>()
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

        val secureLogMessage = "Personen ${token.username} ønsker å $beskrivelse ($operasjon) for identitetsnummer $identitetsnummer"
        val correlationId = call.request.headers[HttpHeaders.XCorrelationId]!!

        if (!gruppetilgangService.kanGjøreOperasjon(operasjon = operasjon, token = token, correlationId = correlationId)) {
            secureLog("Avslått: $secureLogMessage")
            return@post call.respond(HttpStatusCode.Forbidden)
        }

        when (personTilgangService.sjekkTilgang(
            identitetsnummer = identitetsnummer,
            correlationId = correlationId,
            authHeader = authorizationHeader)) {
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
