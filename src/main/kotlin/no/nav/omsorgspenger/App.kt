package no.nav.omsorgspenger

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.ktor.application.*
import io.ktor.auth.Authentication
import io.ktor.auth.authenticate
import io.ktor.client.HttpClient
import io.ktor.client.features.json.JacksonSerializer
import io.ktor.client.features.json.JsonFeature
import io.ktor.config.*
import io.ktor.features.*
import io.ktor.jackson.jackson
import io.ktor.routing.Routing
import io.ktor.util.KtorExperimentalAPI
import no.nav.helse.dusseldorf.ktor.auth.AuthStatusPages
import no.nav.helse.dusseldorf.ktor.auth.allIssuers
import no.nav.helse.dusseldorf.ktor.auth.issuers
import no.nav.helse.dusseldorf.ktor.auth.multipleJwtIssuers
import no.nav.helse.dusseldorf.ktor.auth.withoutAdditionalClaimRules
import no.nav.helse.dusseldorf.ktor.core.*
import no.nav.helse.dusseldorf.ktor.health.HealthReporter
import no.nav.helse.dusseldorf.ktor.health.HealthRoute
import no.nav.helse.dusseldorf.ktor.health.HealthService
import no.nav.helse.dusseldorf.ktor.metrics.MetricsRoute
import no.nav.helse.dusseldorf.oauth2.client.ClientSecretAccessTokenClient
import no.nav.omsorgspenger.api.TilgangApi
import no.nav.omsorgspenger.gruppe.ActiveDirectoryGateway
import no.nav.omsorgspenger.gruppe.ActiveDirectoryService
import no.nav.omsorgspenger.gruppe.GruppeResolver
import no.nav.omsorgspenger.gruppe.GruppetilgangService
import no.nav.omsorgspenger.auth.TokenResolver
import no.nav.omsorgspenger.pdl.PdlClient
import no.nav.omsorgspenger.person.PersonTilgangService
import java.net.URI

fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)
private fun ApplicationConfigValue.scopes() = getString().replace(" ", "").split(",").toSet()

@KtorExperimentalAPI
fun Application.app() {
    install(ContentNegotiation) {
        jackson()
    }
    val issuers = environment.config.issuers().withoutAdditionalClaimRules()

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

    val objectMapper: ObjectMapper = jacksonObjectMapper()
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)

    val pdlConfig = environment.config.config("nav.pdl")
    val azureConfig = environment.config.config("nav.auth.azure")
    val omsorgspengerProxyConfig = environment.config.config("nav.omsorgspenger_proxy")
    val omsorgspengerProxyScopes = omsorgspengerProxyConfig.property("scopes").scopes()

    val httpClient = HttpClient {
        install(JsonFeature) {
            serializer = JacksonSerializer(objectMapper)
        }
    }
    val accessTokenClient = ClientSecretAccessTokenClient(
        clientId = azureConfig.property("client_id").getString(),
        clientSecret = azureConfig.property("client_secret").getString(),
        tokenEndpoint = URI(azureConfig.property("token_endpoint").getString())
    )

    val pdlClient = PdlClient(
        pdlDirect = URI(pdlConfig.property("base_url").getString()) to pdlConfig.property("scopes").scopes(),
        pdlProxy = URI(omsorgspengerProxyConfig.property("pdl_base_url").getString()) to omsorgspengerProxyScopes,
        accessTokenClient = accessTokenClient,
        httpClient = httpClient
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
                    ),
                    openAmIssuers = setOf(
                        issuers.filterKeys { it.alias() == "open-am" }.keys.first().issuer()
                    )
                ),
                gruppetilgangService = GruppetilgangService(
                    gruppeResolver = GruppeResolver(
                        azureGroupMappingPath = environment.config.getRequiredString("nav.azure_gruppemapping_resource_path", secret = false),
                        activeDirectoryService = ActiveDirectoryService(
                            activeDirectoryGateway = ActiveDirectoryGateway(
                                memberOfUrl = URI(omsorgspengerProxyConfig.property("member_of_uri").getString()),
                                accessTokenClient = accessTokenClient,
                                scopes = omsorgspengerProxyScopes
                            )
                        )
                    )
                )
            )
        }
    }
}
