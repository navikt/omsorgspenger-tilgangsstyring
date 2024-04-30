package no.nav.omsorgspenger.pdl

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.nimbusds.jwt.SignedJWT
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import no.nav.helse.dusseldorf.ktor.client.SimpleHttpClient.httpOptions
import no.nav.helse.dusseldorf.ktor.client.SimpleHttpClient.httpPost
import no.nav.helse.dusseldorf.ktor.client.SimpleHttpClient.jsonBody
import no.nav.helse.dusseldorf.ktor.client.SimpleHttpClient.readTextOrThrow
import no.nav.helse.dusseldorf.ktor.health.HealthCheck
import no.nav.helse.dusseldorf.ktor.health.Healthy
import no.nav.helse.dusseldorf.ktor.health.Result
import no.nav.helse.dusseldorf.ktor.health.UnHealthy
import no.nav.helse.dusseldorf.oauth2.client.AccessTokenClient
import no.nav.helse.dusseldorf.oauth2.client.CachedAccessTokenClient
import no.nav.omsorgspenger.auth.Token
import java.net.URI

internal class PdlClient(
    private val pdlUrl: URI,
    private val pdlScopes: Set<String>,
    private val accessTokenClient: AccessTokenClient
) : HealthCheck {

    // https://behandlingskatalog.intern.nav.no/process/purpose/PLEIE_OMSORGS_OG_OPPLAERINGSPENGER/4a1c9324-9c5e-4ddb-ac7f-c55d1dcd9736
    const val OMSORGSPENGER_RAMMEMELDING = "B142"
    private val cachedAccessTokenClient = CachedAccessTokenClient(accessTokenClient)
    private val objectMapper: ObjectMapper = jacksonObjectMapper()
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        .registerModule(JavaTimeModule())

    internal suspend fun hentInfoOmPersoner(
        identitetsnummer: Set<String>,
        correlationId: String,
        token: Token
    ): List<HentPersonResponse> {
        return identitetsnummer.map {
            hentInfoOmPerson(it, correlationId, token)
        }
    }

    private suspend fun hentInfoOmPerson(
        identitetsnummer: String,
        correlationId: String,
        token: Token) : HentPersonResponse {
        val jsonBody = objectMapper.writeValueAsString(hentPersonInfoQuery(identitetsnummer))
        val (httpStatusCode, response) = pdlUrl.graphQl().httpPost { builder ->
            builder.header("Nav-Call-Id", correlationId)
            builder.header("TEMA", "OMS")
            builder.header("Behandlingsnummer", OMSORGSPENGER_RAMMEMELDING)
            builder.accept(ContentType.Application.Json)
            builder.contentType(ContentType.Application.Json)
            token.headers().forEach { (key, value) -> builder.header(key, value) }
            builder.jsonBody(jsonBody)
        }.readTextOrThrow()

        require(httpStatusCode.isSuccess()) {
            "HTTP ${httpStatusCode.value} fra ${pdlUrl.graphQl()}"
        }

        return objectMapper.readValue(response, HentPersonResponse::class.java)
    }

    private fun URI.graphQl() = "$this/graphql"

    private fun Token.headers() = mapOf(
            HttpHeaders.Authorization to cachedAccessTokenClient.getAccessToken(
                scopes = pdlScopes,
                onBehalfOf = this.jwt.token
            ).asAuthoriationHeader()
        )

    override suspend fun check() = Result.merge(
        name = "PdlClient",
        accessTokenCheck("PdlDirect", pdlScopes)
    )

    private fun accessTokenCheck(navn: String, scopes: Set<String>) = kotlin.runCatching {
        val accessTokenResponse = accessTokenClient.getAccessToken(scopes)
        (SignedJWT.parse(accessTokenResponse.accessToken).jwtClaimsSet.getStringArrayClaim("roles")?.toList()
            ?: emptyList()).contains("access_as_application")
    }.fold(
        onSuccess = {
            when (it) {
                true -> Healthy("${navn}AccessTokenCheck", "OK")
                false -> UnHealthy("${navn}AccessTokenCheck", "Feil: Mangler rettigheter")
            }
        },
        onFailure = { UnHealthy("${navn}AccessTokenCheck", "Feil: ${it.message}") }
    )
}
