package no.nav.omsorgspenger.api

enum class Operasjon {
    Visning,
    Endring,
    Unntakshåndtering
}

data class TilgangRequest(
    val identitetsnummer: Set<String> = emptySet(),
    val operasjon: Operasjon,
    val beskrivelse: String) {

    init {
        require(beskrivelse.isNotBlank()) { "beskrivelsen er ikke satt." }
        require(identitetsnummer.all {it.matches(GyldigIdentitetsnummerRegex)}) { "ugyldige identitetsnummer"}
    }

    private companion object {
        private val GyldigIdentitetsnummerRegex = "\\d{11,25}".toRegex()
    }
}
