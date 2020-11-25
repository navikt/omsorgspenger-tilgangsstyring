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
import no.nav.omsorgspenger.config.ServiceUser

internal class PdlClient(
    private val pdlBaseUrl: String,
    accessTokenClient: AccessTokenClient,
    private val serviceUser: ServiceUser,
    private val httpClient: HttpClient,
    private val scopes: Set<String>
)
// TODO: kommenter inn igjen
//    : HealthCheck
{

    private val cachedAccessTokenClient = CachedAccessTokenClient(accessTokenClient)

    suspend fun hentInfoOmPersoner(identer: Set<String>, correlationId: String, authHeader: String): List<HentPersonResponse> {
        return identer.map {
            hentInfoOmPerson(it, correlationId, authHeader)
        }
    }

    private suspend fun hentInfoOmPerson(ident: String, correlationId: String, authHeader: String): HentPersonResponse {
        return httpClient.post<HttpStatement>(pdlBaseUrl) {
            header(HttpHeaders.Authorization, authHeader)
            header("Nav-Consumer-Id", serviceUser.username)
            header("Nav-Call-Id", correlationId)
            header("TEMA", "OMS")
            accept(ContentType.Application.Json)
            contentType(ContentType.Application.Json)
            body = hentPersonInfoQuery(ident)
        }.receive()
    }

//    override suspend fun check() = kotlin.runCatching {
//        httpClient.options<HttpStatement>(pdlBaseUrl) {
//            header(HttpHeaders.Authorization, cachedAccessTokenClient.getAccessToken(scopes))
//        }.execute().status
//    }.fold(
//        onSuccess = { statusCode ->
//            when (HttpStatusCode.OK == statusCode) {
//                true -> Healthy("PdlClient", "OK")
//                false -> UnHealthy("PdlClient", "Feil: Mottok Http Status Code ${statusCode.value}")
//            }
//        },
//        onFailure = {
//            UnHealthy("PdlClient", "Feil: ${it.message}")
//        }
//    )
}
