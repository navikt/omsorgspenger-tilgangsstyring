package no.nav.omsorgspenger.pdl

import com.nimbusds.jwt.SignedJWT
import io.ktor.client.HttpClient
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import no.nav.helse.dusseldorf.ktor.client.SimpleHttpClient.httpGet
import no.nav.helse.dusseldorf.ktor.client.SimpleHttpClient.httpOptions
import no.nav.helse.dusseldorf.ktor.health.HealthCheck
import no.nav.helse.dusseldorf.ktor.health.Healthy
import no.nav.helse.dusseldorf.ktor.health.Result
import no.nav.helse.dusseldorf.ktor.health.UnHealthy
import no.nav.helse.dusseldorf.oauth2.client.AccessTokenClient
import no.nav.helse.dusseldorf.oauth2.client.CachedAccessTokenClient
import no.nav.omsorgspenger.gruppe.ActiveDirectoryGateway.Companion.OpenAmTokenHeader
import no.nav.omsorgspenger.auth.OpenAmToken
import no.nav.omsorgspenger.auth.Token
import java.net.URI

internal class PdlClient(
    private val pdlDirect: Pair<URI, Set<String>>,
    private val pdlProxy: Pair<URI, Set<String>>,
    private val accessTokenClient: AccessTokenClient,
    private val httpClient: HttpClient
) : HealthCheck {

    private val cachedAccessTokenClient = CachedAccessTokenClient(accessTokenClient)

    internal suspend fun hentInfoOmPersoner(
        identitetsnummer: Set<String>,
        correlationId: String,
        token: Token): List<HentPersonResponse> {
        return identitetsnummer.map {
            hentInfoOmPerson(it, correlationId, token)
        }
    }

    private suspend fun hentInfoOmPerson(
        identitetsnummer: String,
        correlationId: String,
        token: Token) : HentPersonResponse {
        return httpClient.post<HttpStatement>(token.graphqlUrl()) {
            header("Nav-Call-Id", correlationId)
            header("TEMA", "OMS")
            accept(ContentType.Application.Json)
            contentType(ContentType.Application.Json)
            token.headers().forEach { (key, value) -> header(key, value) }
            body = hentPersonInfoQuery(identitetsnummer)
        }.receive()
    }

    private fun Token.graphqlUrl() = when (this) {
        is OpenAmToken -> pdlProxy.first
        else -> pdlDirect.first
    }.graphQl()

    private fun URI.graphQl() = "$this/graphql"

    private fun Token.headers() = when (this) {
        is OpenAmToken -> mapOf(
            OpenAmTokenHeader to this.authorizationHeader(),
            HttpHeaders.Authorization to cachedAccessTokenClient.getAccessToken(
                scopes = pdlProxy.second
            ).asAuthoriationHeader()
        )
        else -> mapOf(
            HttpHeaders.Authorization to cachedAccessTokenClient.getAccessToken(
                scopes = pdlDirect.second,
                onBehalfOf = this.jwt.token
            ).asAuthoriationHeader()
        )
    }

    override suspend fun check() = Result.merge(
        name = "PdlClient",
        accessTokenCheck("PdlDirect", pdlDirect.second),
        accessTokenCheck("PdlProxy", pdlProxy.second),
        pingCheck("PdlDirect", pdlDirect),
        pingCheck("PdlProxy", pdlProxy)
    )

    private fun accessTokenCheck(navn: String, scopes: Set<String>) = kotlin.runCatching {
        val accessTokenResponse = accessTokenClient.getAccessToken(scopes)
        (SignedJWT.parse(accessTokenResponse.accessToken).jwtClaimsSet.getStringArrayClaim("roles")?.toList() ?: emptyList()).contains("access_as_application")
    }.fold(
        onSuccess = { when (it) {
            true -> Healthy("${navn}AccessTokenCheck", "OK")
            false -> UnHealthy("${navn}AccessTokenCheck", "Feil: Mangler rettigheter")
        }},
        onFailure = { UnHealthy("${navn}AccessTokenCheck", "Feil: ${it.message}") }
    )

    private suspend fun pingCheck(navn: String, pdl: Pair<URI, Set<String>>) : Result = pdl.first.graphQl().httpOptions {
        it.header(HttpHeaders.Authorization, cachedAccessTokenClient.getAccessToken(pdl.second))
    }.second.fold(
        onSuccess = { when (it.status.isSuccess()) {
            true -> Healthy("${navn}PingCheck", "OK: ${it.readText()}")
            false -> UnHealthy("${navn}PingCheck", "Feil: ${it.status}")
        }},
        onFailure = { UnHealthy("${navn}PingCheck", "Feil: ${it.message}")}
    )
}
