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
        with(testApplicationEngine) {
            assertRequest(
                grupper = setOf("Drift"),
                operasjon = Operasjon.Unntakshåndtering,
                forventetStatusCode = HttpStatusCode.NoContent
            )
        }
    }

    @Test
    fun `Sende med identetsnummer gir 500 feil`() {
        with(testApplicationEngine) {
            assertRequest(
                grupper = setOf("Drift"),
                operasjon = Operasjon.Unntakshåndtering,
                forventetStatusCode = HttpStatusCode.InternalServerError,
                jsonBody = """{"operasjon": "${Operasjon.Unntakshåndtering}","beskrivelse": "noe spennende", "identitetsnummer": ["11111111111"]}"""
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