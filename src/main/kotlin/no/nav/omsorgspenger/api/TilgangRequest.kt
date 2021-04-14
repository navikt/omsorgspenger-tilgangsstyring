package no.nav.omsorgspenger.api

enum class Operasjon {
    Visning,
    Endring,
    Unntakshåndtering
}

data class TilgangRequest(
    val identitetsnummer: Set<String> = emptySet(),
    val operasjon: Operasjon,
    val beskrivelse: String)
