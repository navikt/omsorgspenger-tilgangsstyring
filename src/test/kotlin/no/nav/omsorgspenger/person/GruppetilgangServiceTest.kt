package no.nav.omsorgspenger.person

import com.auth0.jwt.JWT
import com.auth0.jwt.interfaces.DecodedJWT
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import no.nav.helse.dusseldorf.testsupport.jws.AnyIssuer
import no.nav.omsorgspenger.api.Operasjon
import no.nav.omsorgspenger.auth.*
import no.nav.omsorgspenger.gruppe.Gruppe
import no.nav.omsorgspenger.gruppe.GruppeResolver
import no.nav.omsorgspenger.gruppe.GruppetilgangService
import org.junit.jupiter.api.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

internal class GruppetilgangServiceTest {

    private val gruppeResolverMock = mockk<GruppeResolver>()
    private val gruppetilgangService = GruppetilgangService(gruppeResolverMock)

    @Test
    fun `Håndterer Visning`() {
        mockGrupper(emptySet())
        assertFalse(Operasjon.Visning.kanGjøres())
        mockGrupper(setOf(Gruppe.Saksbehandler, Gruppe.Drift))
        assertTrue(Operasjon.Visning.kanGjøres())
        for (gruppe in Gruppe.values()){
            mockGrupper(setOf(gruppe))
            assertTrue(Operasjon.Visning.kanGjøres())
        }
    }

    @Test
    fun `Håndterer Endring`() {
        mockGrupper(emptySet())
        assertFalse(Operasjon.Endring.kanGjøres())
        mockGrupper(setOf(Gruppe.Saksbehandler, Gruppe.Drift))
        assertTrue(Operasjon.Endring.kanGjøres())
        for (gruppe in setOf(Gruppe.Veileder, Gruppe.Oppgavestyrer)){
            mockGrupper(setOf(gruppe))
            assertFalse(Operasjon.Endring.kanGjøres())
        }
    }

    @Test
    fun `Håndterer Unntakshåndtering`() {
        mockGrupper(emptySet())
        assertFalse(Operasjon.Unntakshåndtering.kanGjøres())
        mockGrupper(setOf(Gruppe.Saksbehandler, Gruppe.Overstyrer))
        assertFalse(Operasjon.Unntakshåndtering.kanGjøres())
        for (gruppe in setOf(Gruppe.Overstyrer, Gruppe.Oppgavestyrer, Gruppe.Veileder, Gruppe.Saksbehandler, Gruppe.Beslutter)){
            mockGrupper(setOf(gruppe))
            assertFalse(Operasjon.Unntakshåndtering.kanGjøres())
        }
        mockGrupper(setOf(Gruppe.Drift))
        assertTrue(Operasjon.Unntakshåndtering.kanGjøres())
        mockGrupper(setOf(Gruppe.Drift, Gruppe.Saksbehandler))
        assertTrue(Operasjon.Unntakshåndtering.kanGjøres())
    }

    private fun Operasjon.kanGjøres() = this.let { operasjon ->
        runBlocking { gruppetilgangService.kanGjøreOperasjon(operasjon, token, "") }
    }

    private fun mockGrupper(grupper: Set<Gruppe>) = clearMocks(gruppeResolverMock).also { coEvery {
        gruppeResolverMock.resolve(any(), any())
    }.returns(grupper)}

    private companion object {
        private class TestToken(
            override val jwt: DecodedJWT = JWT.decode(AnyIssuer(issuer = "foo").generateJwt()),
            override val username: String = "foo",
            override val clientId: String = "foo",
            override val erPersonToken: Boolean = true
        ) : Token
        private val token = TestToken()

    }
}