package no.nav.omsorgspenger.auth

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import io.ktor.client.request.*
import io.ktor.http.*
import no.nav.helse.dusseldorf.ktor.client.SimpleHttpClient.httpGet
import no.nav.helse.dusseldorf.ktor.client.SimpleHttpClient.readTextOrThrow
import no.nav.helse.dusseldorf.oauth2.client.AccessTokenClient
import no.nav.helse.dusseldorf.oauth2.client.CachedAccessTokenClient
import org.json.JSONArray
import org.json.JSONObject
import java.net.URI
import java.time.Duration


internal class ActiveDirectoryService(
    private val activeDirectoryGateway: ActiveDirectoryGateway) {
    private val cache: Cache<String, Set<Gruppe>> = Caffeine.newBuilder()
        .expireAfterWrite(Duration.ofMinutes(15))
        .maximumSize(100)
        .build()

    internal suspend fun memberOf(token: OpenAmToken, correlationId: String) : Set<Gruppe> {
        require(token.erPersonToken) { "Kan kun gjøres med person token." }
        return cache.getIfPresent(token.username) ?: activeDirectoryGateway.memberOf(
            token = token,
            correlationId = correlationId)
            .mapNotNull { adGruppe -> activeDirectoryGruppeMapping[adGruppe.toUpperCase()] }
            .toSet()
            .also { cache.put(token.username, it) }
    }

    private companion object {
        private val activeDirectoryGruppeMapping = mapOf(
            "0000-GA-k9-saksbehandler" to Gruppe.Saksbehandler,
            "0000-GA-k9-veileder" to Gruppe.Veileder,
            "0000-GA-k9-beslutter" to Gruppe.Beslutter,
            "0000-GA-k9-oppgavestyrer" to Gruppe.Oppgavestyrer,
            "0000-GA-k9-overstyrer" to Gruppe.Overstyrer,
            "0000-GA-k9-drift" to Gruppe.Drift)
        .mapKeys { it.key.toUpperCase() }
        .also { require(it.values.containsAll(Gruppe.values().toList())) }
    }
}

internal class ActiveDirectoryGateway(
    private val memberOfUrl: URI,
    accessTokenClient: AccessTokenClient,
    private val scopes: Set<String>) {

    private val cachedAccessTokenClient = CachedAccessTokenClient(accessTokenClient)

    internal suspend fun memberOf(token: OpenAmToken, correlationId: String) : Set<String> {
        val authorizationHeader = cachedAccessTokenClient.getAccessToken(scopes).asAuthoriationHeader()
        val (statusCode, response) = memberOfUrl.toString().httpGet {
            it.header(HttpHeaders.Authorization, authorizationHeader)
            it.header(HttpHeaders.XCorrelationId, correlationId)
            it.header(OpenAmTokenHeader, token.authorizationHeader())
        }.readTextOrThrow()
        require(statusCode == HttpStatusCode.OK) {
            "Mottok StatusCode $statusCode ved henting av grupper."
        }
        return JSONObject(response).let { json -> when (json.has("value")) {
            true -> json.getJSONArray("value")
            false -> JSONArray()
        }}.map { it as JSONObject }.map { it.getString("displayName") }.toSet()
    }

    internal companion object {
        internal const val OpenAmTokenHeader = "X-Open-AM"
    }
}