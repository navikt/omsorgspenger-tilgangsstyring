package no.nav.omsorgspenger.pdl

import io.ktor.client.HttpClient
import io.ktor.client.request.accept
import io.ktor.client.request.header
import io.ktor.client.request.options
import io.ktor.client.request.post
import io.ktor.client.statement.HttpStatement
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import no.nav.helse.dusseldorf.ktor.health.HealthCheck
import no.nav.helse.dusseldorf.ktor.health.Healthy
import no.nav.helse.dusseldorf.ktor.health.UnHealthy
import no.nav.helse.dusseldorf.oauth2.client.AccessTokenClient
import no.nav.helse.dusseldorf.oauth2.client.CachedAccessTokenClient
import no.nav.omsorgspenger.gruppe.ActiveDirectoryGateway.Companion.OpenAmTokenHeader
import no.nav.omsorgspenger.auth.OpenAmToken
import no.nav.omsorgspenger.auth.Token
import no.nav.omsorgspenger.config.ServiceUser

internal class PdlClient(
    private val pdlBaseUrl: String,
    accessTokenClient: AccessTokenClient,
    private val serviceUser: ServiceUser,
    private val httpClient: HttpClient,
    private val scopes: Set<String>
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
        return httpClient.post<HttpStatement>(pdlBaseUrl) {
            header("Nav-Consumer-Id", serviceUser.username)
            header("Nav-Call-Id", correlationId)
            header("TEMA", "OMS")
            if (token is OpenAmToken) {
                header(HttpHeaders.Authorization, cachedAccessTokenClient.getAccessToken(scopes).asAuthoriationHeader())
                header(OpenAmTokenHeader, token.authorizationHeader())
            } else {
                header(HttpHeaders.Authorization, token.authorizationHeader())
            }
            accept(ContentType.Application.Json)
            contentType(ContentType.Application.Json)
            body = hentPersonInfoQuery(identitetsnummer)
        }.receive()
    }

    override suspend fun check() = kotlin.runCatching {
        httpClient.options<HttpStatement>(pdlBaseUrl) {
            header(HttpHeaders.Authorization, cachedAccessTokenClient.getAccessToken(scopes).asAuthoriationHeader())
        }.execute().status
    }.fold(
        onSuccess = { statusCode ->
            when (HttpStatusCode.OK == statusCode) {
                true -> Healthy("PdlClient", "OK")
                false -> UnHealthy("PdlClient", "Feil: Mottok Http Status Code ${statusCode.value}")
            }
        },
        onFailure = {
            UnHealthy("PdlClient", "Feil: ${it.message}")
        }
    )
}
