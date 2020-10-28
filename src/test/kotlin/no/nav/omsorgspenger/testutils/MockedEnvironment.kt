package no.nav.omsorgspenger.testutils

import no.nav.helse.dusseldorf.testsupport.jws.Azure
import no.nav.helse.dusseldorf.testsupport.wiremock.WireMockBuilder
import no.nav.helse.dusseldorf.testsupport.wiremock.getAzureV2JwksUrl

internal class MockedEnvironment(
    wireMockPort: Int = 8082
) {

    internal val wireMockServer = WireMockBuilder()
        .withPort(wireMockPort)
        .withAzureSupport()
        .withNaisStsSupport()
        .build()

    internal val appConfig = mutableMapOf<String, String>()

    init {
        // Azure Issuers
        appConfig["nav.auth.issuers.0.alias"] = "azure-v2"
        appConfig["nav.auth.issuers.0.jwks_uri"] = wireMockServer.getAzureV2JwksUrl()
        appConfig["nav.auth.issuers.0.issuer"] = Azure.V2_0.getIssuer()
    }

    internal fun start() = this

    internal fun stop() {
        wireMockServer.stop()
    }
}
