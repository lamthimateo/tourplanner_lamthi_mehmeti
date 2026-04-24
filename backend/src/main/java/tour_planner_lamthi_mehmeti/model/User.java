package tour_planner_lamthi_mehmeti.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * JPA entity representing an application user.
 *
 * <p>Stores the credentials needed to authenticate a user. The table is named
 * {@code app_user} rather than {@code user} because {@code USER} is a reserved
 * keyword in most SQL databases (including H2 and PostgreSQL), and would require
 * quoting in every query if used as a table name.
 *
 * <p>Password storage: the {@link #password} field stores a BCrypt hash, never
 * the plain-text password. The hash is created in {@link tour_planner_lamthi_mehmeti.service.AuthService}
 * using Spring Security's {@code PasswordEncoder} before the entity is saved.
 *
 * <p>Database table: {@code app_user}
 */
@Entity
@Table(name = "app_user")
public class User {

    /**
     * Surrogate primary key, auto-incremented by the database.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The unique login name chosen by the user.
     * Enforced unique at the database level ({@code unique = true}) and also
     * checked in the service layer before attempting an INSERT to provide a
     * clearer error message.
     *
     * <p>Minimum 3 characters, maximum 50 characters.
     */
    @NotBlank
    @Size(min = 3, max = 50)
    @Column(unique = true, nullable = false)
    private String username;

    /**
     * BCrypt hash of the user's password.
     *
     * <p>Named {@code password_hash} in the database column to make it explicit
     * that the stored value is never the raw password. Spring Security's
     * {@code BCryptPasswordEncoder} generates and verifies this hash.
     */
    @NotBlank
    @Column(name = "password_hash", nullable = false)
    private String password;

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    /** Default no-arg constructor required by JPA. */
    public User() {
    }

    /**
     * Convenience constructor for creating a new user account.
     *
     * @param username   the unique login name (3–50 characters)
     * @param password   the BCrypt-hashed password (never plain text)
     */
    public User(String username, String password) {
        this.username = username;
        this.password = password;
    }

    // -------------------------------------------------------------------------
    // Getters and setters
    // -------------------------------------------------------------------------

    /**
     * Returns the database-assigned user ID.
     * This ID is embedded as a claim in the JWT so that downstream services
     * can identify the calling user without an extra database round-trip.
     *
     * @return the user's primary key
     */
    public Long getId() {
        return id;
    }

    /** @param id the surrogate primary key */
    public void setId(Long id) {
        this.id = id;
    }

    /** @return the unique username */
    public String getUsername() {
        return username;
    }

    /** @param username the unique username */
    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * Returns the BCrypt-hashed password.
     * This value is only ever compared using {@code PasswordEncoder#matches};
     * it is never decrypted or returned to the client.
     *
     * @return the BCrypt password hash
     */
    public String getPassword() {
        return password;
    }

    /** @param password the BCrypt-hashed password */
    public void setPassword(String password) {
        this.password = password;
    }
}
