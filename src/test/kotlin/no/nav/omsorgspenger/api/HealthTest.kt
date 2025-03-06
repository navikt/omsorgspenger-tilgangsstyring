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
internal class HealthTest(
    private val mockedEnvironment: MockedEnvironment
) {
    @Test
    fun `isready gir 200`() {
        testApplication {
            environment {
                config = getConfig(mockedEnvironment.appConfig)
            }
            client.get("/isready").apply {
                assertThat(status).isEqualTo(HttpStatusCode.OK)
            }
        }
    }

    @Test
    fun `isalive gir 200`() {
        testApplication {
            environment {
                config = getConfig(mockedEnvironment.appConfig)
            }
            client.get("/isalive").apply {
                assertThat(status).isEqualTo(HttpStatusCode.OK)
            }
        }
    }
}
