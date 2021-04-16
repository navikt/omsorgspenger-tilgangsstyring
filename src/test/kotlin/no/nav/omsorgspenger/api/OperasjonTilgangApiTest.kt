package no.nav.omsorgspenger.api

import io.ktor.http.*
import io.ktor.server.testing.*
import no.nav.omsorgspenger.testutils.TestApplicationExtension

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(TestApplicationExtension::class)
internal class OperasjonTilgangApiTest(
    private val testApplicationEngine: TestApplicationEngine) {

    @Test
    fun `Beslutter forsøker å gjøre unntakshåndtering`() {
        with(testApplicationEngine) {
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
        with(testApplicationEngine) {
            assertRequest(
                grupper = setOf("Drift"),
                operasjon = Operasjon.Unntakshåndtering,
                forventetStatusCode = HttpStatusCode.NoContent
            )
        }
        // identitetsnummer tom liste
        with(testApplicationEngine) {
            assertRequest(
                grupper = setOf("Drift"),
                operasjon = Operasjon.Unntakshåndtering,
                jsonBody = """{"operasjon": "${Operasjon.Unntakshåndtering}","beskrivelse": "noe spennende", "identitetsnummer": []}""",
                forventetStatusCode = HttpStatusCode.NoContent
            )
        }
    }

    private companion object {
        private fun TestApplicationEngine.assertRequest(
            grupper: Set<String>,
            operasjon: Operasjon,
            forventetStatusCode: HttpStatusCode,
            jsonBody: String = """{"operasjon": "$operasjon","beskrivelse": "noe spennende"}""") {
            handleRequest(HttpMethod.Post, "/api/tilgang") {
                addHeader(HttpHeaders.ContentType, "application/json")
                addHeader(HttpHeaders.XCorrelationId, "id")
                addHeader("Authorization", "Bearer ${gyldigToken(grupper = grupper)}")
                setBody(jsonBody)
            }.apply {
                assertThat(response.status()).isEqualTo(forventetStatusCode)
            }
        }
    }
}