package no.nav.omsorgspenger.testutils.mocks

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.containing
import com.github.tomakehurst.wiremock.client.WireMock.equalTo
import com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath
import com.github.tomakehurst.wiremock.matching.AnythingPattern
import no.nav.omsorgspenger.pdl.NotFound
import no.nav.omsorgspenger.pdl.ServerError
import no.nav.omsorgspenger.pdl.Unauthorised

private const val pdlApiBasePath = "/pdlapi-mock"
private const val pdlApiMockPath = "/"

private fun WireMockServer.stubPdlApiHentPerson(): WireMockServer {
    WireMock.stubFor(
        WireMock.post(
            WireMock
                .urlPathMatching(".*$pdlApiMockPath.*")
        )
            .withHeader("Authorization", containing("Bearer"))
            .withHeader("Content-Type", equalTo("application/json"))
            .withHeader("Nav-Consumer-Token", AnythingPattern())
            .withHeader("x-nav-apiKey", AnythingPattern())
            .withRequestBody(matchingJsonPath("$.variables.ident", containing(identSomGirTilgang_1)))
            .willReturn(
                WireMock.aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody(
                        """
                            {
                                "data": {
                                    "hentPerson": {
                                        "bostedsadresse": [
                                            {
                                                "coAdressenavn": "Test"
                                            }
                                        ]
                                    }
                                }
                            }
                        """.trimIndent()
                    )
            )
    )

    return this
}

private fun WireMockServer.stubPdlApiHentPersonWithError(error: String, identitetsnummer: String): WireMockServer {
    WireMock.stubFor(
        WireMock.post(
            WireMock
                .urlPathMatching(".*$pdlApiMockPath.*")
        )
            .withHeader("Authorization", containing("Bearer"))
            .withHeader("Content-Type", equalTo("application/json"))
            .withHeader("Nav-Consumer-Token", AnythingPattern())
            .withHeader("x-nav-apiKey", AnythingPattern())
            .withRequestBody(matchingJsonPath("$.variables.ident", containing(identitetsnummer)))
            .willReturn(
                WireMock.aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody(
                        """
                            {
                              "errors": [
                                {
                                  "message": "Ikke tilgang til å se person",
                                  "locations": [
                                    {
                                      "line": 30,
                                      "column": 5
                                    }
                                  ],
                                  "path": [
                                    "hentPerson"
                                  ],
                                  "extensions": {
                                    "code": "$error",
                                    "classification": "ExecutionAborted"
                                  }
                                }
                              ],
                              "data": null
                            }
                        """.trimIndent()
                    )
            )
    )

    return this
}

private fun WireMockServer.stubPdlApiServerErrorResponse(): WireMockServer {
    WireMock.stubFor(
        WireMock.post(
            WireMock
                .urlPathMatching(".*$pdlApiMockPath.*")
        )
            .withHeader("Authorization", containing("Bearer"))
            .withHeader("Content-Type", equalTo("application/json"))
            .withHeader("Nav-Consumer-Token", AnythingPattern())
            .withHeader("x-nav-apiKey", AnythingPattern())
            .withRequestBody(matchingJsonPath("$.variables.ident", containing("500")))
            .willReturn(
                WireMock.aResponse()
                    .withStatus(500)
            )
    )

    return this
}

internal fun WireMockServer.stubPdlApi() = stubPdlApiHentPersonWithError(Unauthorised, identSomGirUnauthorised)
    .stubPdlApiHentPersonWithError(NotFound, identSomIkkeFinnes)
    .stubPdlApiHentPersonWithError(ServerError, identSomKasterServerError)
    .stubPdlApiHentPerson()
    .stubPdlApiServerErrorResponse()
