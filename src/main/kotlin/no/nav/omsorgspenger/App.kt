package no.nav.omsorgspenger

import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.config.*
import io.ktor.server.plugins.callid.*
import io.ktor.server.plugins.callloging.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.routing.*
import no.nav.helse.dusseldorf.ktor.auth.AuthStatusPages
import no.nav.helse.dusseldorf.ktor.auth.allIssuers
import no.nav.helse.dusseldorf.ktor.auth.issuers
import no.nav.helse.dusseldorf.ktor.auth.multipleJwtIssuers
import no.nav.helse.dusseldorf.ktor.auth.withoutAdditionalClaimRules
import no.nav.helse.dusseldorf.ktor.core.DefaultProbeRoutes
import no.nav.helse.dusseldorf.ktor.core.DefaultStatusPages
import no.nav.helse.dusseldorf.ktor.core.correlationIdAndRequestIdInMdc
import no.nav.helse.dusseldorf.ktor.core.fromXCorrelationIdHeader
import no.nav.helse.dusseldorf.ktor.core.getRequiredString
import no.nav.helse.dusseldorf.ktor.core.logRequests
import no.nav.helse.dusseldorf.ktor.health.HealthReporter
import no.nav.helse.dusseldorf.ktor.health.HealthRoute
import no.nav.helse.dusseldorf.ktor.health.HealthService
import no.nav.helse.dusseldorf.ktor.metrics.MetricsRoute
import no.nav.helse.dusseldorf.oauth2.client.ClientSecretAccessTokenClient
import no.nav.omsorgspenger.api.TilgangApi
import no.nav.omsorgspenger.auth.TokenResolver
import no.nav.omsorgspenger.gruppe.GruppeResolver
import no.nav.omsorgspenger.gruppe.GruppetilgangService
import no.nav.omsorgspenger.pdl.PdlClient
import no.nav.omsorgspenger.person.PersonTilgangService
import java.net.URI

fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)
private fun ApplicationConfigValue.scopes() = getString().replace(" ", "").split(",").toSet()

fun Application.app() {
    val issuers = environment.config.issuers().withoutAdditionalClaimRules()

    install(ContentNegotiation) {
        jackson()
    }

    install(Authentication) {
        multipleJwtIssuers(issuers)
    }

    install(StatusPages) {
        DefaultStatusPages()
        AuthStatusPages()
    }

    install(CallId) {
        fromXCorrelationIdHeader(
            generateOnInvalid = true
        )
    }

    install(CallLogging) {
        correlationIdAndRequestIdInMdc()
        logRequests()
        callIdMdc("callId")
    }

    val pdlConfig = environment.config.config("nav.pdl")
    val azureConfig = environment.config.config("nav.auth.azure")

    val accessTokenClient = ClientSecretAccessTokenClient(
        clientId = azureConfig.property("client_id").getString(),
        clientSecret = azureConfig.property("client_secret").getString(),
        tokenEndpoint = URI(azureConfig.property("token_endpoint").getString()),
        authenticationMode = ClientSecretAccessTokenClient.AuthenticationMode.POST
    )

    val pdlClient = PdlClient(
        pdlUrl = URI(pdlConfig.property("base_url").getString()),
        pdlScopes = pdlConfig.property("scopes").scopes(),
        accessTokenClient = accessTokenClient
    )

    val healthService = HealthService(setOf(pdlClient))

    HealthReporter(
        "omsorgspenger-tilgangsstyring",
        healthService
    )

    install(Routing) {
        HealthRoute(healthService = healthService)
        MetricsRoute()
        DefaultProbeRoutes()
        authenticate(*issuers.allIssuers()) {
            TilgangApi(
                personTilgangService = PersonTilgangService(
                    pdlClient = pdlClient
                ),
                tokenResolver = TokenResolver(
                    azureIssuers = setOf(
                        issuers.filterKeys { it.alias() == "azure-v2" }.keys.first().issuer()
                    )
                ),
                gruppetilgangService = GruppetilgangService(
                    gruppeResolver = GruppeResolver(
                        azureGroupMappingPath = environment!!.config.getRequiredString(
                            "nav.azure_gruppemapping_resource_path",
                            secret = false
                        )
                    )
                )
            )
        }
    }
}
