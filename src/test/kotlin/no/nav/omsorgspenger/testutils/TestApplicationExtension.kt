package no.nav.omsorgspenger.testutils

import com.typesafe.config.ConfigFactory
import io.ktor.config.ApplicationConfig
import io.ktor.config.HoconApplicationConfig
import io.ktor.server.testing.TestApplicationEngine
import io.ktor.server.testing.createTestEnvironment
import io.ktor.util.KtorExperimentalAPI
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.ParameterContext
import org.junit.jupiter.api.extension.ParameterResolver

internal class TestApplicationExtension : ParameterResolver {

    @KtorExperimentalAPI
    internal companion object {
        @KtorExperimentalAPI
        internal val testApplicationEngine = TestApplicationEngine(
            environment = createTestEnvironment {
                config = getConfig(mutableMapOf())
            }
        )

        init {
            testApplicationEngine.start(wait = true)
        }
    }

    private val støttedeParametre = listOf(
        TestApplicationEngine::class.java
    )

    override fun supportsParameter(parameterContext: ParameterContext, extensionContext: ExtensionContext): Boolean {
        return støttedeParametre.contains(parameterContext.parameter.type)
    }

    override fun resolveParameter(parameterContext: ParameterContext, extensionContext: ExtensionContext): Any {
        return testApplicationEngine
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
