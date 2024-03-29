package no.nav.omsorgspenger.testutils

import no.nav.helse.dusseldorf.testsupport.jws.Azure
import no.nav.helse.dusseldorf.testsupport.jws.NaisSts
import no.nav.helse.dusseldorf.testsupport.wiremock.WireMockBuilder
import no.nav.helse.dusseldorf.testsupport.wiremock.getAzureV2JwksUrl
import no.nav.helse.dusseldorf.testsupport.wiremock.getAzureV2TokenUrl
import no.nav.helse.dusseldorf.testsupport.wiremock.getNaisStsJwksUrl
import no.nav.omsorgspenger.testutils.mocks.memberOfUri
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
        appConfig["nav.auth.issuers.0.type"] = "azure"
        appConfig["nav.auth.issuers.0.audience"] = "omsorgspenger-tilgangsstyring"
        appConfig["nav.auth.issuers.0.jwks_uri"] = wireMockServer.getAzureV2JwksUrl()
        appConfig["nav.auth.issuers.0.issuer"] = Azure.V2_0.getIssuer()
        appConfig["nav.auth.azure.client_id"] = "omsorgspenger-tilgangsstyring"
        appConfig["nav.auth.azure.client_secret"] = "anything"
        appConfig["nav.auth.azure.token_endpoint"] = wireMockServer.getAzureV2TokenUrl()
        appConfig["nav.pdl.base_url"] = wireMockServer.pdlApiBaseUrl()
        appConfig["nav.pdl.scopes"] = "pdl/.default"
        appConfig["nav.azure_gruppemapping_resource_path"] = "azureGruppeMapping/test.json"
    }

    internal fun start() = this

    internal fun stop() {
        wireMockServer.stop()
    }
}
