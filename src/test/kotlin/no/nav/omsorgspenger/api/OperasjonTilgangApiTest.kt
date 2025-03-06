package no.nav.omsorgspenger.api

import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.testing.*
import no.nav.omsorgspenger.testutils.MockedEnvironment
import no.nav.omsorgspenger.testutils.TestApplicationExtension
import no.nav.omsorgspenger.testutils.getConfig
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(TestApplicationExtension::class)
internal class OperasjonTilgangApiTest(
    private val mockedEnvironment: MockedEnvironment
) {

    @Test
    fun `Beslutter forsøker å gjøre unntakshåndtering`() {
        testApplication {
            environment {
                config = getConfig(mockedEnvironment.appConfig)
            }
            assertRequest(
                grupper = setOf("Beslutter"),
                operasjon = Operasjon.Unntakshåndtering,
                forventetStatusCode = HttpStatusCode.Forbidden
            )
        }
    }

    @Test
    fun `Driftsperson forsøker å gjøre unntakshåndtering`() {
        // identitetsnummer ikke satt i det hele tatt.
        testApplication {
            environment {
                config = getConfig(mockedEnvironment.appConfig)
            }
            assertRequest(
                grupper = setOf("Drift"),
                operasjon = Operasjon.Unntakshåndtering,
                forventetStatusCode = HttpStatusCode.NoContent
            )
        }
        // identitetsnummer tom liste
        testApplication {
            environment {
                config = getConfig(mockedEnvironment.appConfig)
            }
            assertRequest(
                grupper = setOf("Drift"),
                operasjon = Operasjon.Unntakshåndtering,
                jsonBody = """{"operasjon": "${Operasjon.Unntakshåndtering}","beskrivelse": "noe spennende", "identitetsnummer": []}""",
                forventetStatusCode = HttpStatusCode.NoContent
            )
        }
    }

    private companion object {
        private suspend fun ApplicationTestBuilder.assertRequest(
            grupper: Set<String>,
            operasjon: Operasjon,
            forventetStatusCode: HttpStatusCode,
            jsonBody: String = """{"operasjon": "$operasjon","beskrivelse": "noe spennende"}"""
        ) {

            client.post("/api/tilgang") {
                header(HttpHeaders.ContentType, "application/json")
                header(HttpHeaders.XCorrelationId, "id")
                header("Authorization", "Bearer ${gyldigToken(grupper = grupper)}")
                setBody(jsonBody)
            }.apply {
                assertThat(status).isEqualTo(forventetStatusCode)
            }
        }
    }
}
