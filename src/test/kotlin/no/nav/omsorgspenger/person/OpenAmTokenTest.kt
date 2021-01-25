package no.nav.omsorgspenger.person

import com.github.tomakehurst.wiremock.WireMockServer
import io.ktor.http.*
import io.ktor.server.testing.*
import no.nav.helse.dusseldorf.testsupport.jws.NaisSts
import no.nav.omsorgspenger.testutils.TestApplicationExtension
import no.nav.omsorgspenger.testutils.mocks.identSomGirTilgang_1
import no.nav.omsorgspenger.testutils.mocks.stubMemberOf
import org.assertj.core.api.Assertions.assertThat
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(TestApplicationExtension::class)
internal class OpenAmTokenTest(
    private val testApplicationEngine: TestApplicationEngine,
    private val wireMockServer: WireMockServer) {

    @Test
    fun `Gir 204 ved når man har rett gruppe`() {
        val correlationId = "Correlation-Id-Saksbehandler"
        wireMockServer.stubMemberOf(correlationId)
        with(testApplicationEngine) {
            handleRequest(HttpMethod.Post, "/api/tilgang/personer") {
                addHeader(HttpHeaders.ContentType, "application/json")
                addHeader(HttpHeaders.XCorrelationId, correlationId)
                addHeader("Authorization", "Bearer ${gyldigToken(navIdent = "Ola")}")
                @Language("JSON")
                val jsonBody = """
                    {
                      "identitetsnummer": ["$identSomGirTilgang_1"],
                      "operasjon": "${Operasjon.Visning}",
                      "beskrivelse": "slå opp saksnummer"
                    }
                """.trimIndent()
                setBody(jsonBody)
            }.apply {
                assertThat(response.status()).isEqualTo(HttpStatusCode.NoContent)
            }
        }
    }

    @Test
    fun `Gir 403 når man ikke er medlem av rett gruppe`() {
        val correlationId = "Correlation-Id-UkjentGruppe"
        wireMockServer.stubMemberOf(correlationId)
        with(testApplicationEngine) {
            handleRequest(HttpMethod.Post, "/api/tilgang/personer") {
                addHeader(HttpHeaders.ContentType, "application/json")
                addHeader(HttpHeaders.XCorrelationId, correlationId)
                addHeader("Authorization", "Bearer ${gyldigToken(navIdent = "Kari")}")
                @Language("JSON")
                val jsonBody = """
                    {
                      "identitetsnummer": ["$identSomGirTilgang_1"],
                      "operasjon": "${Operasjon.Visning}",
                      "beskrivelse": "slå opp saksnummer"
                    }
                """.trimIndent()
                setBody(jsonBody)
            }.apply {
                assertThat(response.status()).isEqualTo(HttpStatusCode.Forbidden)
            }
        }
    }

    private fun gyldigToken(navIdent:String) = NaisSts.generateJwt(
        application = "myApplication",
        overridingClaims = mapOf(
            "tokenName" to "id_token",
            "sub" to navIdent,
            "azp" to "myClient"
        )
    )
}