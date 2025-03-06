package no.nav.omsorgspenger.testutils

import com.github.tomakehurst.wiremock.WireMockServer
import com.typesafe.config.ConfigFactory
import io.ktor.server.config.*
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.ParameterContext
import org.junit.jupiter.api.extension.ParameterResolver

internal class TestApplicationExtension : ParameterResolver {

    internal companion object {
        private val mockedEnvironment = MockedEnvironment().start()

        init {
            Runtime.getRuntime().addShutdownHook(
                Thread {
                    mockedEnvironment.stop()
                }
            )
        }
    }

    private val støttedeParametre = listOf(
        MockedEnvironment::class.java,
        WireMockServer::class.java
    )

    override fun supportsParameter(parameterContext: ParameterContext, extensionContext: ExtensionContext): Boolean {
        return støttedeParametre.contains(parameterContext.parameter.type)
    }

    override fun resolveParameter(parameterContext: ParameterContext, extensionContext: ExtensionContext): Any {
        return when (parameterContext.parameter.type) {
            MockedEnvironment::class.java -> mockedEnvironment
            else -> mockedEnvironment.wireMockServer
        }
    }
}

internal fun getConfig(config: MutableMap<String, String>): ApplicationConfig {
    config.medAppConfig(8083)
    val fileConfig = ConfigFactory.load()
    val testConfig = ConfigFactory.parseMap(config)
    val mergedConfig = testConfig.withFallback(fileConfig)
    return HoconApplicationConfig(mergedConfig)
}

internal fun MutableMap<String, String>.medAppConfig(port: Int) = also {
    it.putIfAbsent("ktor.deployment.port", "$port")
}
