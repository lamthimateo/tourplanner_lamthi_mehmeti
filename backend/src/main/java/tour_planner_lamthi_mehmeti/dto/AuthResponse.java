package tour_planner_lamthi_mehmeti.dto;

/**
 * DTO returned by {@code POST /api/auth/register} and
 * {@code POST /api/auth/login}.
 *
 * <p>The frontend stores the {@code token} in {@code localStorage} and attaches
 * it to every subsequent request via the HTTP {@code Authorization: Bearer ...}
 * header (see the Angular {@code authInterceptor}). {@code username} is
 * echoed back so the UI can greet the user without decoding the JWT.
 *
 * <p>No password or password-hash is ever returned — the server only surfaces
 * the minimum the client needs to maintain a session.
 */
public class AuthResponse {

    /** Signed JWT (HS256) issued by {@code JwtUtil.generateToken}. */
    private String token;

    /** Username of the authenticated user, echoed back for UI convenience. */
    private String username;

    /** Default constructor required by Jackson for JSON (de)serialization. */
    public AuthResponse() {
    }

    /** Convenience constructor used by {@code AuthService} when building a response. */
    public AuthResponse(String token, String username) {
        this.token = token;
        this.username = username;
    }

    /** @return the JWT the client should send on subsequent requests */
    public String getToken() { return token; }
    /** @param token the JWT to return to the client */
    public void setToken(String token) { this.token = token; }

    /** @return the authenticated username */
    public String getUsername() { return username; }
    /** @param username the authenticated username */
    public void setUsername(String username) { this.username = username; }
}
