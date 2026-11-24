package co.unicauca.saberpro.common.security;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/** Token de servicio (client credentials): se reutiliza mientras este vigente y se renueva antes de vencer. */
class ServiceTokenProviderTest {

    /** Reloj que se puede adelantar en las pruebas. */
    private static final class MutableClock extends Clock {
        private Instant now = Instant.parse("2026-11-23T10:00:00Z");

        void advanceSeconds(long seconds) {
            now = now.plusSeconds(seconds);
        }

        @Override
        public ZoneOffset getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }

    private final MutableClock clock = new MutableClock();
    private final AtomicInteger calls = new AtomicInteger();
    private final ServiceTokenProvider provider = new ServiceTokenProvider(
            () -> new ServiceTokenProvider.IssuedToken("token-" + calls.incrementAndGet(), 300), clock);

    @Test
    void reutilizaElTokenMientrasEsteVigente() {
        assertEquals("token-1", provider.accessToken());
        clock.advanceSeconds(200);
        assertEquals("token-1", provider.accessToken());
        assertEquals(1, calls.get());
    }

    @Test
    void loRenuevaCuandoEstaPorVencer() {
        provider.accessToken();
        clock.advanceSeconds(300 - 29);
        assertEquals("token-2", provider.accessToken());
    }

    @Test
    void invalidarObligaAPedirOtro() {
        provider.accessToken();
        provider.invalidate();
        assertEquals("token-2", provider.accessToken());
    }

    @Test
    void fallaSiKeycloakNoDevuelveToken() {
        ServiceTokenProvider broken = new ServiceTokenProvider(
                () -> new ServiceTokenProvider.IssuedToken(" ", 300), clock);
        assertThrows(IllegalStateException.class, broken::accessToken);
    }
}
