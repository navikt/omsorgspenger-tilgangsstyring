package no.nav.omsorgspenger.pdl

data class GraphqlQuery(
    val query: String,
    val variables: Variables
)

data class PersonInfoGraphqlQuery(
    val query: String,
    val variables: Variables
)

data class Variables(
    val identer: List<String>
)

data class PdlError(
    val message: String,
    val locations: List<PdlErrorLocation>,
    val path: List<String>?,
    val extensions: PdlErrorExtension
)

data class PdlErrorLocation(
    val line: Int?,
    val column: Int?
)

data class PdlErrorExtension(
    val code: String?,
    val classification: String
)

const val Unauthenticated = "unauthenticated"
const val Unauthorised = "unauthorized"
const val NotFound = "not_found"
const val BadRequest = "bad_request"
const val ServerError = "server_error"

fun hentPersonInfoQuery(fnr: Set<String>): PersonInfoGraphqlQuery {
    val query = GraphqlQuery::class.java.getResource("/pdl/hentPersonBolkInfo.graphql").readText().replace("[\n\r]", "")
    return PersonInfoGraphqlQuery(query, Variables(fnr.toList()))
}
