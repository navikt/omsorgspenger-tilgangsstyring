package no.nav.omsorgspenger.person

import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.TestApplicationEngine
import io.ktor.server.testing.handleRequest
import io.ktor.server.testing.setBody
import no.nav.omsorgspenger.testutils.TestApplicationExtension
import org.assertj.core.api.Assertions.assertThat
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(TestApplicationExtension::class)
internal class PersonTilgangApiTest(
    private val testApplicationEngine: TestApplicationEngine
) {
    @Test
    internal fun `Gir 204 ved oppgitt identitetsnumre og operasjon`() {
        with(testApplicationEngine) {
            handleRequest(HttpMethod.Post, "/api/tilgang/personer") {
                addHeader(HttpHeaders.ContentType, "application/json")
                @Language("JSON")
                val jsonBody = """
                    {
                      "identitetsnumre": ["01010101011", "12312312312"],
                      "operasjon": "Visning"
                    }
                """.trimIndent()
                setBody(jsonBody)
            }.apply {
                assertThat(response.status()).isEqualTo(HttpStatusCode.NoContent)
            }
        }
    }

    @Test
    internal fun `Request uten body gir 400`() {
        with(testApplicationEngine) {
            handleRequest(HttpMethod.Post, "/api/tilgang/personer") {
                addHeader(HttpHeaders.ContentType, "application/json")
                setBody("{}")
            }.apply {
                assertThat(response.status()).isEqualTo(HttpStatusCode.BadRequest)
            }
        }
    }
}
