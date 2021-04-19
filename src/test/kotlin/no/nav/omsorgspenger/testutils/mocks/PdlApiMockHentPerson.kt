package no.nav.omsorgspenger.testutils.mocks

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.containing
import com.github.tomakehurst.wiremock.client.WireMock.equalTo
import com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath
import com.github.tomakehurst.wiremock.matching.AnythingPattern
import io.ktor.http.HttpHeaders
import no.nav.omsorgspenger.pdl.NotFound
import no.nav.omsorgspenger.pdl.ServerError
import no.nav.omsorgspenger.pdl.Unauthorised

private const val pdlApiMockPath = "/"

private fun WireMockServer.stubPdlApiHentPerson(identitetsnummer: String): WireMockServer {
    WireMock.stubFor(
        WireMock.post(
            WireMock
                .urlPathMatching(".*$pdlApiMockPath.*")
        )
            .withHeader(HttpHeaders.Authorization, containing("Bearer"))
            .withHeader(HttpHeaders.ContentType, equalTo("application/json"))
            .withHeader("Nav-Call-Id", AnythingPattern())
            .withHeader("TEMA", equalTo("OMS"))
            .withRequestBody(matchingJsonPath("$.variables.ident", containing(identitetsnummer)))
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
                                    "classification": "ExecutionAborted",
                                    "details": {
                                        "type": "abac-deny",
                                        "cause": "cause-0001-manglerrolle",
                                        "policy": "skjermede_navansatte_og_familiemedlemmer"
                                    }
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
    .stubPdlApiHentPerson(identSomGirTilgang_1)
    .stubPdlApiHentPerson(identSomGirTilgang_2)
    .stubPdlApiServerErrorResponse()
