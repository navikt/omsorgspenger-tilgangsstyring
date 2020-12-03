package no.nav.omsorgspenger.person

import com.fasterxml.jackson.databind.exc.MismatchedInputException
import com.fasterxml.jackson.module.kotlin.MissingKotlinParameterException
import io.ktor.application.call
import io.ktor.auth.jwt.JWTPrincipal
import io.ktor.auth.principal
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.routing.post
import no.nav.omsorgspenger.Gruppetilgang
import no.nav.omsorgspenger.secureLog
import org.slf4j.LoggerFactory

internal fun Route.PersonTilgangApi(
    personTilgangService: PersonTilgangService,
    gruppetilgang: Gruppetilgang) {

    val logger = LoggerFactory.getLogger("no.nav.omsorgspenger.person.PersonTilgangApi")

    post("/api/tilgang/personer") {
        val jwt = call.principal<JWTPrincipal>() ?: return@post call.respond(HttpStatusCode.Unauthorized)
        val authorizationHeader = requireNotNull(call.request.headers[HttpHeaders.Authorization])

        if (!jwt.erPersonbruker()) {
            val clientId = jwt.payload.claims["azp"]?.asString()
            logger.warn("Tilgangsstyring ble kalt fra et system og ikke en personbruker. ClientId: $clientId")
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

        val username = jwt.payload.claims["preferred_username"]?.asString()
        val logMessage = "Personen $username ønsker å $beskrivelse ($operasjon) for identitetsnummer $identitetsnummer"
        val correlationId = call.request.headers[HttpHeaders.XCorrelationId]!!

        if (!gruppetilgang.kanGjøreOperasjon(jwt = jwt, operasjon = operasjon)) {
            secureLog("Avslått: $logMessage")
            return@post call.respond(HttpStatusCode.Forbidden)
        }

        when (personTilgangService.sjekkTilgang(
            identitetsnummer = identitetsnummer,
            correlationId = correlationId,
            authHeader = authorizationHeader)) {
            true -> {
                secureLog("Innvilget: $logMessage")
                call.respond(HttpStatusCode.NoContent)
            }
            false -> {
                secureLog("Avslått: $logMessage")
                call.respond(HttpStatusCode.Forbidden)
            }
        }
    }
}


private fun JWTPrincipal.erPersonbruker() =
    this.payload.claims.contains("oid") && this.payload.claims.contains("preferred_username")
