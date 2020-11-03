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
import no.nav.omsorgspenger.secureLog
import org.slf4j.LoggerFactory

internal fun Route.PersonTilgangApi(personTilgangService: PersonTilgangService) {

    val logger = LoggerFactory.getLogger("no.nav.omsorgspenger.person.PersonTilgangApi")

    post("/api/tilgang/personer") {
        val jwt = call.principal<JWTPrincipal>().also {
            if (it == null) {
                return@post call.respond(HttpStatusCode.Unauthorized)
            }
        }

        if (!jwt!!.erPersonbruker()) {
            val systemId = jwt.payload.claims["azp"]?.asString()
            logger.warn("personer-api ble kalt fra et system og ikke en personbruker. System-id: $systemId")
            return@post call.respond(HttpStatusCode.Forbidden)
        }

        val authHeader = call.request.headers[HttpHeaders.Authorization]!!

        try {
            val (identitetsnummer, operasjon, beskrivelse) = call.receive<PersonerRequestBody>()

            val username = jwt.payload.claims["preferred_username"]?.asString()
            val logMessage = "Personen $username ønsker å $beskrivelse ($operasjon) for personidenter $identitetsnummer"

            // TODO: correlationId
            when (personTilgangService.sjekkTilgang(identitetsnummer, "TODO", authHeader)) {
                true -> {
                    secureLog("Innvilget: $logMessage")
                    call.respond(HttpStatusCode.NoContent)
                }
                false -> {
                    secureLog("Avslått: $logMessage")
                    call.respond(HttpStatusCode.Forbidden)
                }
            }
            // TODO: bake dette inn i alle apikall
        } catch (ex: Throwable) {
            when (ex) {
                is MismatchedInputException,
                is MissingKotlinParameterException -> {
                    logger.error("Mapping exception", ex)
                    call.respond(HttpStatusCode.BadRequest, ex.localizedMessage)
                }
                else -> call.respond(HttpStatusCode.InternalServerError, ex.localizedMessage)
            }
        }
    }
}

internal fun JWTPrincipal.erPersonbruker() =
    this.payload.claims.contains("oid") && this.payload.claims.contains("preferred_username")
