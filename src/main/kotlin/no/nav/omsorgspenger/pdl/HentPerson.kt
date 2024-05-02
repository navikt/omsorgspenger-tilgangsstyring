package no.nav.omsorgspenger.pdl

data class HentPersonResponse(
    val data: Data?,
    val errors: List<PdlError>?
)

data class Data(
    val hentPerson: HentPerson?
)

data class HentPerson(
    val folkeregisteridentifikator: List<Folkeregisteridentifikator>
)

data class Folkeregisteridentifikator(
    val identifikasjonsnummer: String
)
