package no.nav.omsorgspenger.api

import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.TestApplicationEngine
import io.ktor.server.testing.handleRequest
import io.ktor.server.testing.setBody
import no.nav.helse.dusseldorf.testsupport.jws.Azure
import no.nav.omsorgspenger.testutils.TestApplicationExtension
import no.nav.omsorgspenger.testutils.mocks.identSomGirTilgang_1
import no.nav.omsorgspenger.testutils.mocks.identSomGirTilgang_2
import no.nav.omsorgspenger.testutils.mocks.identSomGirUnauthorised
import no.nav.omsorgspenger.testutils.mocks.identSomIkkeFinnes
import no.nav.omsorgspenger.testutils.mocks.identSomKasterServerError
import org.assertj.core.api.Assertions.assertThat
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.util.*

@ExtendWith(TestApplicationExtension::class)
internal class PersonTilgangApiTest(
    private val testApplicationEngine: TestApplicationEngine
) {

    @Test
    fun `Gir 204 ved oppgitt identitetsnummer og operasjon`() {
        with(testApplicationEngine) {
            handleRequest(HttpMethod.Post, "/api/tilgang/personer") {
                addHeader(HttpHeaders.ContentType, "application/json")
                addHeader(HttpHeaders.XCorrelationId, UUID.randomUUID().toString())
                addHeader("Authorization", "Bearer ${gyldigToken(grupper = setOf("Beslutter"))}")
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
    fun `Gir 204 ved flere gyldige identitetsnumre`() {
        with(testApplicationEngine) {
            handleRequest(HttpMethod.Post, "/api/tilgang/personer") {
                addHeader(HttpHeaders.ContentType, "application/json")
                addHeader(HttpHeaders.XCorrelationId, UUID.randomUUID().toString())
                addHeader("Authorization", "Bearer ${gyldigToken(grupper = setOf("Overstyrer", "UkjentGruppe"))}")
                @Language("JSON")
                val jsonBody = """
                    {
                      "identitetsnummer": ["$identSomGirTilgang_1", "$identSomGirTilgang_2"],
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
    fun `Gir 500 ved ugyldig format på identitetsnummer`() {
        with(testApplicationEngine) {
            handleRequest(HttpMethod.Post, "/api/tilgang/personer") {
                addHeader(HttpHeaders.ContentType, "application/json")
                addHeader(HttpHeaders.XCorrelationId, UUID.randomUUID().toString())
                addHeader("Authorization", "Bearer ${gyldigToken(grupper = setOf("Overstyrer", "UkjentGruppe"))}")
                @Language("JSON")
                val jsonBody = """
                    {
                      "identitetsnummer": ["1111111111", "1111111111111111111111111111111"],
                      "operasjon": "${Operasjon.Visning}",
                      "beskrivelse": "slå opp saksnummer"
                    }
                """.trimIndent()
                setBody(jsonBody)
            }.apply {
                assertThat(response.status()).isEqualTo(HttpStatusCode.InternalServerError)
            }
        }
    }

    @Test
    fun `Gir 403 om man ikke har rett gruppe`() {
        with(testApplicationEngine) {
            handleRequest(HttpMethod.Post, "/api/tilgang/personer") {
                addHeader(HttpHeaders.ContentType, "application/json")
                addHeader(HttpHeaders.XCorrelationId, UUID.randomUUID().toString())
                addHeader("Authorization", "Bearer ${gyldigToken(grupper = setOf("UkjentGruppe"))}")
                @Language("JSON")
                val jsonBody = """
                    {
                      "identitetsnummer": ["$identSomGirTilgang_1", "$identSomGirTilgang_2"],
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

    @Test
    fun `Gir 403 dersom man ikke har tilgang til minst ett identitetsnummer`() {
        with(testApplicationEngine) {
            handleRequest(HttpMethod.Post, "/api/tilgang/personer") {
                addHeader(HttpHeaders.ContentType, "application/json")
                addHeader(HttpHeaders.XCorrelationId, UUID.randomUUID().toString())
                addHeader("Authorization", "Bearer ${gyldigToken(grupper = setOf("Veileder", "Saksbehandler"))}")
                @Language("JSON")
                val jsonBody = """
                    {
                      "identitetsnummer": ["$identSomGirTilgang_1", "$identSomIkkeFinnes", "$identSomGirUnauthorised"],
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

    @Test
    fun `Request uten body gir 400`() {
        with(testApplicationEngine) {
            handleRequest(HttpMethod.Post, "/api/tilgang/personer") {
                addHeader(HttpHeaders.ContentType, "application/json")
                addHeader("Authorization", "Bearer ${gyldigToken(grupper = setOf("Beslutter"))}")
                setBody("{}")
            }.apply {
                assertThat(response.status()).isEqualTo(HttpStatusCode.BadRequest)
            }
        }
    }

    @Test
    fun `Gir 401 dersom token ikke er satt`() {
        with(testApplicationEngine) {
            handleRequest(HttpMethod.Post, "/api/tilgang/personer") {
                addHeader(HttpHeaders.ContentType, "application/json")
            }.apply {
                assertThat(response.status()).isEqualTo(HttpStatusCode.Unauthorized)
            }
        }
    }

    @Test
    fun `Gir 403 dersom token har feil audience`() {
        with(testApplicationEngine) {
            handleRequest(HttpMethod.Post, "/api/tilgang/personer") {
                addHeader(HttpHeaders.ContentType, "application/json")
                addHeader("Authorization", "Bearer ${gyldigToken(grupper = setOf("Beslutter"), audience = "any")}")
            }.apply {
                assertThat(response.status()).isEqualTo(HttpStatusCode.Forbidden)
            }
        }
    }

    @Test
    fun `Gir 403 dersom token ikke er utstedt til en personbruker`() {
        val tokenUtenPersonbrukerClaims = Azure.V2_0.generateJwt(
            clientId = "any",
            clientAuthenticationMode = Azure.ClientAuthenticationMode.CLIENT_SECRET,
            audience = "any",
            overridingClaims = mapOf(
                "azp" to "systembruker"
            )
        )
        with(testApplicationEngine) {
            handleRequest(HttpMethod.Post, "/api/tilgang/personer") {
                addHeader(HttpHeaders.ContentType, "application/json")
                addHeader(HttpHeaders.XCorrelationId, "id")
                addHeader("Authorization", "Bearer $tokenUtenPersonbrukerClaims")
            }.apply {
                assertThat(response.status()).isEqualTo(HttpStatusCode.Forbidden)
            }
        }
    }

    @Test
    fun `Gir 403 dersom ikke tilgang til person`() {
        with(testApplicationEngine) {
            handleRequest(HttpMethod.Post, "/api/tilgang/personer") {
                addHeader(HttpHeaders.ContentType, "application/json")
                addHeader("Authorization", "Bearer ${gyldigToken(grupper = setOf("Beslutter"))}")
                addHeader(HttpHeaders.XCorrelationId, "id")
                @Language("JSON")
                val jsonBody = """
                    {
                      "identitetsnummer": ["$identSomGirUnauthorised"],
                      "operasjon": "${Operasjon.Visning}",
                      "beskrivelse": "slå opp rammemeldinger"
                    }
                """.trimIndent()
                setBody(jsonBody)
            }.apply {
                assertThat(response.status()).isEqualTo(HttpStatusCode.Forbidden)
            }
        }
    }

    @Test
    fun `Gir tilgang dersom person ikke finnes`() {
        with(testApplicationEngine) {
            handleRequest(HttpMethod.Post, "/api/tilgang/personer") {
                addHeader(HttpHeaders.ContentType, "application/json")
                addHeader("Authorization", "Bearer ${gyldigToken(grupper = setOf("Beslutter"))}")
                addHeader(HttpHeaders.XCorrelationId, "id")
                @Language("JSON")
                val jsonBody = """
                    {
                      "identitetsnummer": ["$identSomIkkeFinnes"],
                      "operasjon": "${Operasjon.Visning}",
                      "beskrivelse": "slå opp rammemeldinger"
                    }
                """.trimIndent()
                setBody(jsonBody)
            }.apply {
                assertThat(response.status()).isEqualTo(HttpStatusCode.NoContent)
            }
        }
    }

    @Test
    fun `Kaster feil dersom server_error i PDL response error object`() {
        with(testApplicationEngine) {
            handleRequest(HttpMethod.Post, "/api/tilgang/personer") {
                addHeader(HttpHeaders.ContentType, "application/json")
                addHeader(HttpHeaders.XCorrelationId, "id")
                addHeader("Authorization", "Bearer ${gyldigToken(grupper = setOf("Beslutter"))}")
                @Language("JSON")
                val jsonBody = """
                    {
                      "identitetsnummer": ["$identSomKasterServerError"],
                      "operasjon": "${Operasjon.Visning}",
                      "beskrivelse": "slå opp rammemeldinger"
                    }
                """.trimIndent()
                setBody(jsonBody)
            }.apply {
                assertThat(response.status()).isEqualTo(HttpStatusCode.InternalServerError)
            }
        }
    }
}

internal fun gyldigToken(
    grupper: Set<String>,
    audience: String = "omsorgspenger-tilgangsstyring"
) = Azure.V2_0.generateJwt(
    clientId = "any",
    clientAuthenticationMode = Azure.ClientAuthenticationMode.CLIENT_SECRET,
    audience = audience,
    groups = grupper,
    overridingClaims = mapOf(
        "oid" to "any",
        "preferred_username" to "Test Brukersen"
    )
)
