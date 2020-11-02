package no.nav.omsorgspenger.pdl

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
