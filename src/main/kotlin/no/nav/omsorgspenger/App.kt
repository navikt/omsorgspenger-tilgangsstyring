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
import no.nav.omsorgspenger.config.ServiceUser
import no.nav.omsorgspenger.pdl.PdlClient
import no.nav.omsorgspenger.person.PersonTilgangApi
import no.nav.omsorgspenger.person.PersonTilgangService
import no.nav.omsorgspenger.sts.StsRestClient

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

    val objectMapper: ObjectMapper = jacksonObjectMapper()
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

    val pdlConfig = environment.config.config("nav.pdl")
    val stsConfig = environment.config.config("nav.sts")
    val srvConfig = environment.config.config("nav.service_user")

    val serviceUser = ServiceUser(
        username = srvConfig.property("srv_username").getString(),
        password = srvConfig.property("srv_password").getString()
    )
    val httpClient = HttpClient {
        install(JsonFeature) {
            serializer = JacksonSerializer(objectMapper)
        }
    }
    val stsRestClient = StsRestClient(
        stsTokenUrl = stsConfig.property("sts_token_url").getString(),
        stsApiGwKey = stsConfig.property("sts_api_gw_key").getString(),
        serviceUser = serviceUser,
        httpClient = httpClient
    )
    val pdlClient = PdlClient(
        pdlBaseUrl = pdlConfig.property("pdl_base_url").getString(),
        pdlApiKey = pdlConfig.property("pdl_api_gw_key").getString(),
        stsRestClient = stsRestClient,
        serviceUser = serviceUser,
        httpClient = httpClient
    )

    install(Routing) {
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
