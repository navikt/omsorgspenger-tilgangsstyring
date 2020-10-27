package no.nav.omsorgspenger

import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.TestApplicationEngine
import io.ktor.server.testing.handleRequest
import no.nav.omsorgspenger.testutils.TestApplicationExtension
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(TestApplicationExtension::class)
internal class HealthTest(
    private val testApplicationEngine: TestApplicationEngine
) {
    @Test
    internal fun `isready gir 200`() {
        with(testApplicationEngine) {
            handleRequest(HttpMethod.Get, "/isready") {}.apply {
                assertThat(response.status()).isEqualTo(HttpStatusCode.OK)
            }
        }
    }

    @Test
    internal fun `isalive gir 200`() {
        with(testApplicationEngine) {
            handleRequest(HttpMethod.Get, "/isalive") {}.apply {
                assertThat(response.status()).isEqualTo(HttpStatusCode.OK)
            }
        }
    }
}
