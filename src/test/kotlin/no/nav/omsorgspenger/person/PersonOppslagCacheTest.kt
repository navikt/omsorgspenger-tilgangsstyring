package no.nav.omsorgspenger.person

import no.nav.omsorgspenger.person.PersonTilgangService.Companion.personOppslagCache
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class PersonOppslagCacheTest {

    @Test
    fun `Rekkefølgen på identitetsnummer er likegyldig`() {
        val cache = personOppslagCache()
        val cacheKey1 = PersonTilgangService.Companion.CacheKey(
            username = "1",
            identitetsnummer = setOf("1", "2")
        )
        val cacheKey2 = PersonTilgangService.Companion.CacheKey(
            username = "1",
            identitetsnummer = setOf("2", "1")
        )
        val cacheKey3 = PersonTilgangService.Companion.CacheKey(
            username = "1",
            identitetsnummer = setOf("2", "1", "0")
        )
        assertEquals(cacheKey1, cacheKey2)
        cache.put(cacheKey1, true)
        assertNotNull(cache.getIfPresent(cacheKey2))
        assertTrue(cache.getIfPresent(cacheKey2)!!)
        assertNull(cache.getIfPresent(cacheKey3))
        assertNull(cache.getIfPresent(cacheKey1.copy(username = "2")))
    }
}