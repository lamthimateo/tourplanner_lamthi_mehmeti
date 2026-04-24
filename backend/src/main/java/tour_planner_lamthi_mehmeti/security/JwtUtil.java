package tour_planner_lamthi_mehmeti.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;

/**
 * Thin wrapper around the JJWT library that produces and verifies JSON Web
 * Tokens for this application.
 *
 * <p>A JWT has three parts — header, payload (claims) and signature —
 * separated by dots. The signature is computed with the server's secret key
 * (HS256), so any tampering on the client side is immediately detectable:
 * parsing with the same key will throw a {@code SignatureException}.
 *
 * <p>The payload we care about contains:
 * <ul>
 *   <li>{@code sub}      — the username (standard "subject" claim)</li>
 *   <li>{@code userId}   — custom claim used to scope database queries</li>
 *   <li>{@code iat}/{@code exp} — issued-at and expiration timestamps</li>
 * </ul>
 *
 * <p>Configuration comes from {@code application.properties} / environment
 * variables so that secrets aren't baked into the binary. The defaults are
 * only suitable for local development.
 *
 * <p>Used by {@link JwtAuthFilter} on every protected request and by
 * {@code AuthService} to mint tokens after successful login / register.
 */
@Component
public class JwtUtil {

    /** Secret key derived from the configured {@code jwt.secret} string. */
    private final Key key;

    /** Token lifetime in milliseconds (default: 24 h). */
    private final long expirationMs;

    /**
     * Constructor — receives its configuration from Spring's property binder.
     *
     * @param secret       raw string used to derive the HMAC-SHA256 key
     * @param expirationMs how long a freshly issued token remains valid
     */
    public JwtUtil(
            @Value("${jwt.secret:DefaultSuperSecretKeyForTourPlanner2026!!}") String secret,
            @Value("${jwt.expiration-ms:86400000}") long expirationMs) {
        // Keys.hmacShaKeyFor enforces a minimum key length (>= 256 bits for HS256);
        // JJWT will throw if the provided secret is too short.
        this.key = Keys.hmacShaKeyFor(secret.getBytes());
        this.expirationMs = expirationMs;
    }

    /**
     * Mints a new signed JWT carrying {@code username} and {@code userId}.
     *
     * @param username login name placed in the {@code sub} claim
     * @param userId   primary key used for per-user data scoping
     * @return compact (dot-separated) JWT string
     */
    public String generateToken(String username, Long userId) {
        return Jwts.builder()
                .setSubject(username)
                .claim("userId", userId)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expirationMs))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    /** @return the {@code sub} (username) claim of the given token */
    public String getUsername(String token) {
        return getClaims(token).getSubject();
    }

    /** @return the {@code userId} claim of the given token */
    public Long getUserId(String token) {
        return getClaims(token).get("userId", Long.class);
    }

    /**
     * Validates a token end-to-end: parses it, verifies the signature and
     * checks the expiration date. Any failure is swallowed and reported as
     * {@code false}, so callers don't have to catch exceptions in their
     * happy-path code.
     */
    public boolean isValid(String token) {
        try {
            getClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * Parses the token and returns the claim set. Delegates all the hard work
     * (signature verification + expiration check) to JJWT.
     */
    private Claims getClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}
