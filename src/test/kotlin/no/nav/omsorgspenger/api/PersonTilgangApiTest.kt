package no.nav.omsorgspenger.api

import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.testing.*
import no.nav.helse.dusseldorf.testsupport.jws.Azure
import no.nav.omsorgspenger.testutils.MockedEnvironment
import no.nav.omsorgspenger.testutils.TestApplicationExtension
import no.nav.omsorgspenger.testutils.getConfig
import no.nav.omsorgspenger.testutils.mocks.*
import org.assertj.core.api.Assertions.assertThat
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.util.*

@ExtendWith(TestApplicationExtension::class)
internal class PersonTilgangApiTest(
    private val mockedEnvironment: MockedEnvironment
) {

    @Test
    fun `Gir 204 ved oppgitt identitetsnummer og operasjon`() {
        testApplication {
            environment {
                config = getConfig(mockedEnvironment.appConfig)
            }
            client.post("/api/tilgang/personer") {
                header(HttpHeaders.ContentType, "application/json")
                header(HttpHeaders.XCorrelationId, UUID.randomUUID().toString())
                header("Authorization", "Bearer ${gyldigToken(grupper = setOf("Beslutter"))}")
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
                assertThat(status).isEqualTo(HttpStatusCode.NoContent)
            }
        }
    }

    @Test
    fun `Gir 204 ved flere gyldige identitetsnumre`() {
        testApplication {
            environment {
                config = getConfig(mockedEnvironment.appConfig)
            }
            client.post("/api/tilgang/personer") {
                header(HttpHeaders.ContentType, "application/json")
                header(HttpHeaders.XCorrelationId, UUID.randomUUID().toString())
                header("Authorization", "Bearer ${gyldigToken(grupper = setOf("Overstyrer", "UkjentGruppe"))}")
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
                assertThat(status).isEqualTo(HttpStatusCode.NoContent)
            }
        }
    }

    @Test
    fun `Gir 500 ved ugyldig format på identitetsnummer`() {
        testApplication {
            environment {
                config = getConfig(mockedEnvironment.appConfig)
            }
            client.post("/api/tilgang/personer") {
                header(HttpHeaders.ContentType, "application/json")
                header(HttpHeaders.XCorrelationId, UUID.randomUUID().toString())
                header("Authorization", "Bearer ${gyldigToken(grupper = setOf("Overstyrer", "UkjentGruppe"))}")
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
                assertThat(status).isEqualTo(HttpStatusCode.InternalServerError)
            }
        }
    }

    @Test
    fun `Gir 403 om man ikke har rett gruppe`() {
        testApplication {
            environment {
                config = getConfig(mockedEnvironment.appConfig)
            }
            client.post("/api/tilgang/personer") {
                header(HttpHeaders.ContentType, "application/json")
                header(HttpHeaders.XCorrelationId, UUID.randomUUID().toString())
                header("Authorization", "Bearer ${gyldigToken(grupper = setOf("UkjentGruppe"))}")
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
                assertThat(status).isEqualTo(HttpStatusCode.Forbidden)
            }
        }
    }

    @Test
    fun `Gir 403 dersom man ikke har tilgang til minst ett identitetsnummer`() {
        testApplication {
            environment {
                config = getConfig(mockedEnvironment.appConfig)
            }
            client.post("/api/tilgang/personer") {
                header(HttpHeaders.ContentType, "application/json")
                header(HttpHeaders.XCorrelationId, UUID.randomUUID().toString())
                header("Authorization", "Bearer ${gyldigToken(grupper = setOf("Veileder", "Saksbehandler"))}")
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
                assertThat(status).isEqualTo(HttpStatusCode.Forbidden)
            }
        }
    }

    @Test
    fun `Request uten body gir 400`() {
        testApplication {
            environment {
                config = getConfig(mockedEnvironment.appConfig)
            }
            client.post("/api/tilgang/personer") {
                header(HttpHeaders.ContentType, "application/json")
                header("Authorization", "Bearer ${gyldigToken(grupper = setOf("Beslutter"))}")
                setBody("{}")
            }.apply {
                assertThat(status).isEqualTo(HttpStatusCode.BadRequest)
            }
        }
    }

    @Test
    fun `Gir 401 dersom token ikke er satt`() {
        testApplication {
            environment {
                config = getConfig(mockedEnvironment.appConfig)
            }
            client.post("/api/tilgang/personer") {
                header(HttpHeaders.ContentType, "application/json")
            }.apply {
                assertThat(status).isEqualTo(HttpStatusCode.Unauthorized)
            }
        }
    }

    @Test
    fun `Gir 403 dersom token har feil audience`() {
        testApplication {
            environment {
                config = getConfig(mockedEnvironment.appConfig)
            }
            client.post("/api/tilgang/personer") {
                header(HttpHeaders.ContentType, "application/json")
                header("Authorization", "Bearer ${gyldigToken(grupper = setOf("Beslutter"), audience = "any")}")
            }.apply {
                assertThat(status).isEqualTo(HttpStatusCode.Forbidden)
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
        testApplication {
            environment {
                config = getConfig(mockedEnvironment.appConfig)
            }
            client.post("/api/tilgang/personer") {
                header(HttpHeaders.ContentType, "application/json")
                header(HttpHeaders.XCorrelationId, "id")
                header("Authorization", "Bearer $tokenUtenPersonbrukerClaims")
            }.apply {
                assertThat(status).isEqualTo(HttpStatusCode.Forbidden)
            }
        }
    }

    @Test
    fun `Gir 403 dersom ikke tilgang til person`() {
        testApplication {
            environment {
                config = getConfig(mockedEnvironment.appConfig)
            }
            client.post("/api/tilgang/personer") {
                header(HttpHeaders.ContentType, "application/json")
                header("Authorization", "Bearer ${gyldigToken(grupper = setOf("Beslutter"))}")
                header(HttpHeaders.XCorrelationId, "id")
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
                assertThat(status).isEqualTo(HttpStatusCode.Forbidden)
            }
        }
    }

    @Test
    fun `Gir tilgang dersom person ikke finnes`() {
        testApplication {
            environment {
                config = getConfig(mockedEnvironment.appConfig)
            }
            client.post("/api/tilgang/personer") {
                header(HttpHeaders.ContentType, "application/json")
                header("Authorization", "Bearer ${gyldigToken(grupper = setOf("Beslutter"))}")
                header(HttpHeaders.XCorrelationId, "id")
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
                assertThat(status).isEqualTo(HttpStatusCode.NoContent)
            }
        }
    }

    @Test
    fun `Kaster feil dersom server_error i PDL response error object`() {
        testApplication {
            environment {
                config = getConfig(mockedEnvironment.appConfig)
            }
            client.post("/api/tilgang/personer") {
                header(HttpHeaders.ContentType, "application/json")
                header(HttpHeaders.XCorrelationId, "id")
                header("Authorization", "Bearer ${gyldigToken(grupper = setOf("Beslutter"))}")
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
                assertThat(status).isEqualTo(HttpStatusCode.InternalServerError)
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
