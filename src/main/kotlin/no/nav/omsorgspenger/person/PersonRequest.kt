package no.nav.omsorgspenger.person

enum class Operasjon {
    Visning,
    Endring,
    Unntakshåndtering
}

data class PersonerRequestBody(
    val identitetsnummer: Set<String>,
    val operasjon: Operasjon,
    val beskrivelse: String
)
