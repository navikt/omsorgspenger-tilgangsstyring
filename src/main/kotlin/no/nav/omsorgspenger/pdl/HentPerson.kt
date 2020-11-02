package no.nav.omsorgspenger.pdl

data class HentPersonResponse(
    val data: Data?,
    val errors: List<PdlError>?
)

data class Data(
    val hentPerson: HentPerson?
)

data class HentPerson(
    val bostedsadresse: List<Bostedsadresse>
)

data class Bostedsadresse(
    val coAdressenavn: String?
)
