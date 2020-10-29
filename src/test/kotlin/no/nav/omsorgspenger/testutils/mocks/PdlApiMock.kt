package no.nav.omsorgspenger.testutils.mocks

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.containing
import com.github.tomakehurst.wiremock.client.WireMock.equalTo
import com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath
import com.github.tomakehurst.wiremock.matching.AnythingPattern

private const val pdlApiBasePath = "/pdlapi-mock"
private const val pdlApiMockPath = "/"

private fun WireMockServer.stubPdlApiHentPersonBolk(): WireMockServer {
    WireMock.stubFor(
        WireMock.post(
            WireMock
                .urlPathMatching(".*$pdlApiMockPath.*")
        )
            .withHeader("Authorization", containing("Bearer"))
            .withHeader("Content-Type", equalTo("application/json"))
            .withHeader("Nav-Consumer-Token", AnythingPattern())
            .withHeader("x-nav-apiKey", AnythingPattern())
            .withRequestBody(matchingJsonPath("$.variables.identer", containing("01010101011")))
            .withRequestBody(matchingJsonPath("$.variables.identer", containing("12312312312")))
            .willReturn(
                WireMock.aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody(
                        """
                            {
                                "data": {
                                    "hentPersonBolk": [
                                        {
                                            "ident": "12345678910",
                                            "person": {
                                                "navn": [
                                                    {
                                                        "fornavn": "LITEN",
                                                        "mellomnavn": null,
                                                        "etternavn": "MASKIN"
                                                    }
                                                ],
                                                "foedsel": [
                                                    {
                                                        "foedselsdato": "1990-07-04"
                                                    },
                                                    {
                                                        "foedselsdato": "1990-07-04"
                                                    },
                                                    {
                                                        "foedselsdato": "1990-07-04"
                                                    }
                                                ],
                                                "adressebeskyttelse": [
                                                    {
                                                        "gradering": "UGRADERT"
                                                    }
                                                ]
                                            },
                                            "code": "ok"
                                        },
                                        {
                                            "ident": "12345678911",
                                            "person": null,
                                            "code": "not_found"
                                        }
                                    ],
                                    "hentIdenterBolk": [
                                        {
                                            "ident": "12345678910",
                                            "identer": [
                                                {
                                                    "ident": "2722577091065"
                                                }
                                            ],
                                            "code": "ok"
                                        },
                                        {
                                            "ident": "12345678911",
                                            "identer": null,
                                            "code": "not_found"
                                        }
                                    ]
                                }
                            }
                        """.trimIndent()
                    )
            )
    )

    return this
}

private fun WireMockServer.stubPdlApiHentPersonError(): WireMockServer {
    WireMock.stubFor(
        WireMock.post(
            WireMock
                .urlPathMatching(".*$pdlApiMockPath.*")
        )
            .withHeader("Authorization", containing("Bearer"))
            .withHeader("Content-Type", equalTo("application/json"))
            .withHeader("Nav-Consumer-Token", AnythingPattern())
            .withHeader("x-nav-apiKey", AnythingPattern())
            .withRequestBody(matchingJsonPath("$.variables.identer", containing("404")))
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
                                    "code": "unauthorized",
                                    "classification": "ExecutionAborted"
                                  }
                                }
                              ],
                              "data": {
                                "hentPersonBolk": null,
                                "hentIdenterBolk": null
                              }
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
            .withRequestBody(matchingJsonPath("$.variables.identer", containing("500")))
            .willReturn(
                WireMock.aResponse()
                    .withStatus(500)
            )
    )

    return this
}

internal fun WireMockServer.stubPdlApi() = stubPdlApiHentPersonError()
    .stubPdlApiHentPersonBolk()
    .stubPdlApiServerErrorResponse()

internal fun WireMockServer.pdlApiBaseUrl(): String = baseUrl() + pdlApiBasePath
