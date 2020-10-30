package no.nav.omsorgspenger.sts

import io.ktor.client.HttpClient
import io.ktor.client.call.receive
import io.ktor.client.features.ResponseException
import io.ktor.client.request.accept
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.HttpStatement
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.util.toByteArray
import no.nav.helse.dusseldorf.ktor.health.HealthCheck
import no.nav.helse.dusseldorf.ktor.health.Healthy
import no.nav.helse.dusseldorf.ktor.health.UnHealthy
import no.nav.omsorgspenger.config.ServiceUser
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.LocalDateTime

internal class StsRestClient(
    private val stsTokenUrl: String,
    private val stsApiGwKey: String,
    private val serviceUser: ServiceUser,
    private val httpClient: HttpClient = HttpClient()
) : HealthCheck {

    private val logger: Logger = LoggerFactory.getLogger(StsRestClient::class.java)
    private var cachedOidcToken: Token? = null

    internal suspend fun token(): String {
        if (cachedOidcToken?.expired != false) cachedOidcToken = fetchToken()
        return cachedOidcToken!!.access_token
    }

    private suspend fun fetchToken(): Token {
        return kotlin.runCatching {
            httpClient.get<HttpStatement>("$stsTokenUrl?grant_type=client_credentials&scope=openid") {
                header(HttpHeaders.Authorization, serviceUser.basicAuth)
                header("x-nav-apiKey", stsApiGwKey)
                accept(ContentType.Application.Json)
            }.execute()
        }.håndterResponse()
    }

    private suspend fun Result<HttpResponse>.håndterResponse(): Token = fold(
        onSuccess = { response ->
            when (response.status) {
                HttpStatusCode.OK -> response.receive()
                else -> {
                    response.logIt()
                    throw RuntimeException("Uventet statuskode ved henting av token")
                }
            }
        },
        onFailure = { cause ->
            when (cause is ResponseException) {
                true -> {
                    cause.response.logIt()
                    throw RuntimeException("Uventet feil ved henting av token")
                }
                else -> throw cause
            }
        }
    )

    private suspend fun HttpResponse.logIt() =
        logger.error("HTTP ${status.value} fra STS, response: ${String(content.toByteArray())}")

    private data class Token(
        val access_token: String,
        private val token_type: String,
        private val expires_in: Long
    ) {
        // expire 10 seconds before actual expiry. for great margins.
        private val expirationTime: LocalDateTime = LocalDateTime.now().plusSeconds(expires_in - 10L)
        val expired get() = expirationTime.isBefore(LocalDateTime.now())
    }

    override suspend fun check() = kotlin.runCatching {
        fetchToken()
    }.fold(
        onSuccess = { Healthy("StsRestClient", "OK") },
        onFailure = { UnHealthy("StsRestClient", "Feil: ${it.message}") }
    )
}
