package no.nav.omsorgspenger

import io.ktor.application.Application
import io.ktor.application.install
import io.ktor.features.ContentNegotiation
import io.ktor.jackson.jackson
import io.ktor.routing.Routing
import no.nav.helse.dusseldorf.ktor.core.DefaultProbeRoutes
import no.nav.omsorgspenger.person.PersonTilgangApi
import no.nav.omsorgspenger.person.PersonTilgangService

fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

fun Application.app() {
    install(ContentNegotiation) {
        jackson()
    }

    install(Routing) {
        DefaultProbeRoutes()
        PersonTilgangApi(
            personTilgangService = PersonTilgangService()
        )
    }
}
