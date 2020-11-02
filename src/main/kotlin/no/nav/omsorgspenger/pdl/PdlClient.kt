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
import no.nav.omsorgspenger.config.ServiceUser
import no.nav.omsorgspenger.sts.StsRestClient

internal class PdlClient(
    private val pdlBaseUrl: String,
    private val pdlApiKey: String,
    private val stsRestClient: StsRestClient,
    private val serviceUser: ServiceUser,
    private val httpClient: HttpClient
) : HealthCheck {

    suspend fun hentInfoOmPersoner(identer: Set<String>, correlationId: String, authHeader: String): HentPdlResponse {
        return httpClient.post<HttpStatement>(pdlBaseUrl) {
            header(HttpHeaders.Authorization, authHeader)
            header("Nav-Consumer-Token", "Bearer ${stsRestClient.token()}")
            header("Nav-Consumer-Id", serviceUser.username)
            header("Nav-Call-Id", correlationId)
            header("TEMA", "OMS")
            header("x-nav-apiKey", pdlApiKey)
            accept(ContentType.Application.Json)
            contentType(ContentType.Application.Json)
            body = hentPersonInfoQuery(identer)
        }.receive()
    }

    override suspend fun check() = kotlin.runCatching {
        httpClient.options<HttpStatement>(pdlBaseUrl) {
            header(HttpHeaders.Authorization, "Bearer ${stsRestClient.token()}")
            header("x-nav-apiKey", pdlApiKey)
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
