package no.nav.omsorgspenger.testutils

import no.nav.helse.dusseldorf.testsupport.jws.Azure
import no.nav.helse.dusseldorf.testsupport.wiremock.WireMockBuilder
import no.nav.helse.dusseldorf.testsupport.wiremock.getAzureV2JwksUrl
import no.nav.helse.dusseldorf.testsupport.wiremock.getNaisStsTokenUrl
import no.nav.omsorgspenger.testutils.mocks.pdlApiBaseUrl
import no.nav.omsorgspenger.testutils.mocks.stubPdlApi

internal class MockedEnvironment(
    wireMockPort: Int = 8082
) {

    internal val wireMockServer = WireMockBuilder()
        .withPort(wireMockPort)
        .withAzureSupport()
        .withNaisStsSupport()
        .build()
        .stubPdlApi()

    internal val appConfig = mutableMapOf<String, String>()

    init {
        appConfig["nav.auth.issuers.0.alias"] = "azure-v2"
        appConfig["nav.auth.issuers.0.jwks_uri"] = wireMockServer.getAzureV2JwksUrl()
        appConfig["nav.auth.issuers.0.issuer"] = Azure.V2_0.getIssuer()
        appConfig["nav.sts.sts_token_url"] = wireMockServer.getNaisStsTokenUrl()
        appConfig["nav.sts.sts_api_gw_key"] = "testApiKeySts"
        appConfig["nav.pdl.pdl_base_url"] = wireMockServer.pdlApiBaseUrl()
        appConfig["nav.pdl.pdl_api_gw_key"] = "testPdlApiKey"
        appConfig["nav.service_user.srv_username"] = "test_username"
        appConfig["nav.service_user.srv_password"] = "test_pw"
    }

    internal fun start() = this

    internal fun stop() {
        wireMockServer.stop()
    }
}
