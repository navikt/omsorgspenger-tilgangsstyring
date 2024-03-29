package no.nav.omsorgspenger.auth

import com.auth0.jwt.JWT
import com.auth0.jwt.interfaces.DecodedJWT
import io.ktor.server.application.*
import io.ktor.http.*
import org.slf4j.LoggerFactory

internal class TokenResolver(
    private val azureIssuers: Set<String>
) {
    internal fun resolve(call: ApplicationCall): Pair<String, Token>? {
        val header = call.request.headers[HttpHeaders.Authorization] ?: return logger.warn("Ingen Authorization header")
            .let { null }
        val decodedJwt = JWT.decode(header.removePrefix("Bearer "))
        return when (decodedJwt.issuer) {
            in azureIssuers -> header to AzureToken.parse(decodedJwt)
            else -> logger.warn("Støtter ikke issuer ${decodedJwt.issuer}").let { null }
        }
    }

    private companion object {
        private val logger = LoggerFactory.getLogger(TokenResolver::class.java)
    }
}

internal interface Token {
    val jwt: DecodedJWT
    val username: String
    val clientId: String
    val erPersonToken: Boolean
    fun authorizationHeader() = "Bearer ${jwt.token}"
}

internal class AzureToken private constructor(
    override val jwt: DecodedJWT,
    override val clientId: String,
    private val oid: String?,
    private val preferredUsername: String?,
    private val navIdent: String?
) : Token {
    internal companion object {
        internal fun parse(jwt: DecodedJWT) = AzureToken(
            jwt = jwt,
            clientId = jwt.claims.getValue("azp").asString(),
            oid = jwt.claims["oid"]?.asString(),
            preferredUsername = jwt.claims["preferred_username"]?.asString(),
            navIdent = jwt.claims["NAVident"]?.asString()
        )
    }

    override val erPersonToken = oid != null && preferredUsername != null
    override val username = navIdent ?: preferredUsername ?: "n/a"
}