package no.nav.omsorgspenger.person

import com.fasterxml.jackson.databind.exc.MismatchedInputException
import com.fasterxml.jackson.module.kotlin.MissingKotlinParameterException
import io.ktor.application.call
import io.ktor.auth.jwt.JWTPrincipal
import io.ktor.auth.principal
import io.ktor.http.HttpStatusCode
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.routing.post

internal fun Route.PersonTilgangApi(personTilgangService: PersonTilgangService) {
    post("/api/tilgang/personer") {
        val jwt = call.principal<JWTPrincipal>().also {
            if (it == null) {
                return@post call.respond(HttpStatusCode.Unauthorized)
            }
        }
        if (!jwt!!.erPersonbruker()) {
            return@post call.respond(HttpStatusCode.Unauthorized)
        }

        try {
            val (identitetsnumre, operasjon) = call.receive<PersonerRequestBody>()

            when (personTilgangService.sjekkTilgang(identitetsnumre, operasjon)) {
                true -> call.respond(HttpStatusCode.NoContent)
                false -> call.respond(HttpStatusCode.Forbidden)
            }
            // TODO: bake dette inn i alle apikall
        } catch (ex: Throwable) {
            when (ex) {
                is MismatchedInputException,
                is MissingKotlinParameterException -> {
                    call.respond(HttpStatusCode.BadRequest, ex.localizedMessage)
                }
                else -> throw ex
            }
        }
    }
}

internal fun JWTPrincipal.erPersonbruker() =
    this.payload.claims.contains("oid") && this.payload.claims.contains("preferred_username")
