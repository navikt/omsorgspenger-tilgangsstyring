package no.nav.omsorgspenger.testutils.mocks

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import io.ktor.http.*

private const val memberOfPath = "/memberOf"

internal fun WireMockServer.memberOfUri() = "${baseUrl()}$memberOfPath"
internal fun WireMockServer.stubMemberOf(correlationId: String): WireMockServer {
    val body = correlationIdResponseMapping[correlationId]
    WireMock.stubFor(
        WireMock.get(WireMock.urlPathEqualTo(memberOfPath))
            .withHeader(HttpHeaders.Authorization, WireMock.containing("Bearer "))
            .withHeader(HttpHeaders.XCorrelationId, WireMock.equalTo(correlationId))
            .withHeader("X-Open-AM", WireMock.containing("Bearer "))
            .willReturn(
                WireMock.aResponse()
                    .withStatus(
                        when (body) {
                            null -> 404
                            else -> 200
                        }
                    )
                    .withHeader("Content-Type", "application/json")
                    .withBody(body)
            )
    )
    return this
}

private val correlationIdResponseMapping = mapOf(
    "Correlation-Id-Saksbehandler" to """{"value": [{ "displayName": "0000-GA-k9-saksbehandler" }]}""",
    "Correlation-Id-UkjentGruppe" to """{"value": [{ "displayName": "UkjentGruppe" }]}"""
)