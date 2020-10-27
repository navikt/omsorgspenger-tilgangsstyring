package no.nav.omsorgspenger.person

enum class Operasjon {
    Visning
}

data class PersonerRequestBody(
    val identitetsnumre: List<String>,
    val operasjon: Operasjon
)
