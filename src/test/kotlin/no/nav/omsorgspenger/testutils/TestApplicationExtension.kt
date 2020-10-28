package no.nav.omsorgspenger.testutils

import com.github.tomakehurst.wiremock.WireMockServer
import com.typesafe.config.ConfigFactory
import io.ktor.config.ApplicationConfig
import io.ktor.config.HoconApplicationConfig
import io.ktor.server.engine.stop
import io.ktor.server.testing.TestApplicationEngine
import io.ktor.server.testing.createTestEnvironment
import io.ktor.util.KtorExperimentalAPI
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.ParameterContext
import org.junit.jupiter.api.extension.ParameterResolver
import java.util.concurrent.TimeUnit

internal class TestApplicationExtension : ParameterResolver {

    @KtorExperimentalAPI
    internal companion object {
        private val mockedEnvironment = MockedEnvironment().start()
        @KtorExperimentalAPI
        internal val testApplicationEngine = TestApplicationEngine(
            environment = createTestEnvironment {
                config = getConfig(mockedEnvironment.appConfig)
            }
        )

        init {
            testApplicationEngine.start(wait = true)
            Runtime.getRuntime().addShutdownHook(
                Thread {
                    testApplicationEngine.stop(10, 60, TimeUnit.SECONDS)
                    mockedEnvironment.stop()
                }
            )
        }
    }

    private val støttedeParametre = listOf(
        TestApplicationEngine::class.java,
        WireMockServer::class.java
    )

    override fun supportsParameter(parameterContext: ParameterContext, extensionContext: ExtensionContext): Boolean {
        return støttedeParametre.contains(parameterContext.parameter.type)
    }

    @KtorExperimentalAPI
    override fun resolveParameter(parameterContext: ParameterContext, extensionContext: ExtensionContext): Any {
        return when (parameterContext.parameter.type) {
            TestApplicationEngine::class.java -> testApplicationEngine
            else -> mockedEnvironment.wireMockServer
        }
    }
}

@KtorExperimentalAPI
private fun getConfig(config: MutableMap<String, String>): ApplicationConfig {
    config.medAppConfig(8083)
    val fileConfig = ConfigFactory.load()
    val testConfig = ConfigFactory.parseMap(config)
    val mergedConfig = testConfig.withFallback(fileConfig)
    return HoconApplicationConfig(mergedConfig)
}

internal fun MutableMap<String, String>.medAppConfig(port: Int) = also {
    it.putIfAbsent("ktor.deployment.port", "$port")
}
