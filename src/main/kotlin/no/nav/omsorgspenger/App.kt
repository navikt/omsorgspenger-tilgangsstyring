package no.nav.omsorgspenger

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.ktor.application.Application
import io.ktor.application.install
import io.ktor.auth.Authentication
import io.ktor.auth.authenticate
import io.ktor.client.HttpClient
import io.ktor.client.features.json.JacksonSerializer
import io.ktor.client.features.json.JsonFeature
import io.ktor.features.CallId
import io.ktor.features.ContentNegotiation
import io.ktor.features.StatusPages
import io.ktor.jackson.jackson
import io.ktor.routing.Routing
import io.ktor.util.KtorExperimentalAPI
import no.nav.helse.dusseldorf.ktor.auth.AuthStatusPages
import no.nav.helse.dusseldorf.ktor.auth.allIssuers
import no.nav.helse.dusseldorf.ktor.auth.issuers
import no.nav.helse.dusseldorf.ktor.auth.multipleJwtIssuers
import no.nav.helse.dusseldorf.ktor.auth.withoutAdditionalClaimRules
import no.nav.helse.dusseldorf.ktor.core.DefaultProbeRoutes
import no.nav.helse.dusseldorf.ktor.core.DefaultStatusPages
import no.nav.helse.dusseldorf.ktor.core.fromXCorrelationIdHeader
import no.nav.helse.dusseldorf.ktor.health.HealthRoute
import no.nav.helse.dusseldorf.ktor.health.HealthService
import no.nav.helse.dusseldorf.oauth2.client.ClientSecretAccessTokenClient
import no.nav.omsorgspenger.config.ServiceUser
import no.nav.omsorgspenger.pdl.PdlClient
import no.nav.omsorgspenger.person.PersonTilgangApi
import no.nav.omsorgspenger.person.PersonTilgangService
import java.net.URI

fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

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

    val objectMapper: ObjectMapper = jacksonObjectMapper()
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

    val pdlConfig = environment.config.config("nav.pdl")
    val srvConfig = environment.config.config("nav.service_user")
    val azureConfig = environment.config.config("nav.auth.azure")

    val serviceUser = ServiceUser(
        username = srvConfig.property("srv_username").getString(),
        password = srvConfig.property("srv_password").getString()
    )
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
        pdlBaseUrl = pdlConfig.property("pdl_base_url").getString(),
        accessTokenClient = accessTokenClient,
        serviceUser = serviceUser,
        httpClient = httpClient,
        scopes = setOf(environment.config.property("nav.omsorgspenger_proxy.scope").getString())
    )
    val healthService = HealthService(
        setOf(
            pdlClient
        )
    )

    install(Routing) {
        HealthRoute(healthService = healthService)
        DefaultProbeRoutes()
        authenticate(*issuers.allIssuers()) {
            PersonTilgangApi(
                personTilgangService = PersonTilgangService(
                    pdlClient = pdlClient
                )
            )
        }
    }
}
