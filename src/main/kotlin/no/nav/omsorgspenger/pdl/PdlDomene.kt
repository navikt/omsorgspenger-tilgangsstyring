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
    val ident: String
)

fun hentPersonInfoQuery(ident: String): PersonInfoGraphqlQuery {
    val query = GraphqlQuery::class.java.getResource("/pdl/hentPerson.graphql").readText().replace("[\n\r]", "")
    return PersonInfoGraphqlQuery(query, Variables(ident))
}
