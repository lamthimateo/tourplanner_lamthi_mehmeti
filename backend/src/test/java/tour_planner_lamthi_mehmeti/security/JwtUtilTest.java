package tour_planner_lamthi_mehmeti.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Unit tests for JWT create/parse/validate. */
class JwtUtilTest {

    private static final String SECRET = "UnitTestSecretKey-MustBeLongEnoughForHmacSha256!!";

    private JwtUtil newUtil(long expirationMs) {
        return new JwtUtil(SECRET, expirationMs);
    }

    @Test
    void generatedTokenCanBeParsedForBothSubjectAndUserId() {
        JwtUtil util = newUtil(60_000);
        String token = util.generateToken("alice", 42L);

        assertEquals("alice", util.getUsername(token));
        assertEquals(42L, util.getUserId(token));
        assertTrue(util.isValid(token));
    }

    @Test
    void tokensFromDifferentSecretsAreRejected() {
        JwtUtil util = newUtil(60_000);
        JwtUtil other = new JwtUtil("AnotherSecretKeyThatIsAlsoLongEnough12345!", 60_000);

        String token = util.generateToken("bob", 1L);
        assertFalse(other.isValid(token));
    }

    @Test
    void malformedTokenIsInvalid() {
        JwtUtil util = newUtil(60_000);
        assertFalse(util.isValid("not.a.real-token"));
        assertFalse(util.isValid(""));
    }

    @Test
    void expiredTokenIsInvalid() throws Exception {
        JwtUtil util = newUtil(1); // 1 millisecond lifetime
        String token = util.generateToken("carol", 5L);
        Thread.sleep(10);
        assertFalse(util.isValid(token));
    }

    @Test
    void tokenWithTamperedPayloadIsInvalid() {
        JwtUtil util = newUtil(60_000);
        String token = util.generateToken("dave", 9L);
        // Change the last character of the signature segment → signature mismatch
        String tampered = token.substring(0, token.length() - 1) + (token.endsWith("A") ? "B" : "A");
        assertFalse(util.isValid(tampered));
    }
}
