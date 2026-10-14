package edu.harvard.iq.keycloak.auth.spi.fixtures;

import edu.harvard.iq.keycloak.auth.spi.models.DataverseAuthenticatedUser;
import edu.harvard.iq.keycloak.auth.spi.models.DataverseBuiltinUser;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceConfiguration;
import org.hibernate.cfg.AvailableSettings;
import org.mindrot.jbcrypt.BCrypt;

import java.time.Duration;
import java.util.List;

import static java.lang.System.Logger.Level.INFO;

/**
 * Creates the Dataverse user schema from the entity mappings and seeds it with test users.
 * <p>
 * Usage options:
 * <ul>
 *     <li>{@link #seed(String, String, String, List)}: self-contained one-shot: build EMF, create schema, seed, close.</li>
 *     <li>{@link #createEntityManagerFactory(String, String, String)} + {@link #seed(EntityManagerFactory, List)}
 *         or {@link #reset(EntityManagerFactory, List)}: caller owns the EMF lifecycle (e.g. one per test class).</li>
 *     <li>{@link #seed(EntityManager, List)} / {@link #deleteAll(EntityManager)}: caller owns the EM and its transaction.</li>
 * </ul>
 */
public final class DataverseTestData {
    
    private static final System.Logger LOG = System.getLogger(DataverseTestData.class.getName());
    
    /**
     *  Password encryption version used by Dataverse for bcrypt hashes.
     *  TODO: VERIFY THE VERSION UPSTREAM!!!
     */
    private static final int BCRYPT_VERSION = 1;
    
    public record TestUser(int id, String username, String password, String email, String firstName, String lastName) {}
    
    public static final TestUser ALICE = new TestUser(1, "alice", "secret", "alice@example.org", "Alice", "Liddell");
    
    public static final List<TestUser> ALL_USERS = List.of(ALICE);
    
    private DataverseTestData() {
        // utility class
    }
    
    // --- One-shot API (unchanged signatures) ---
    
    /**
     * Drops and recreates the tables for all mapped entities, then persists the given users.
     */
    public static void seed(String jdbcUrl, String username, String password, TestUser... users) {
        seed(jdbcUrl, username, password, List.of(users));
    }
    
    public static void seed(String jdbcUrl, String username, String password, List<TestUser> users) {
        LOG.log(INFO, "Seeding Dataverse test data: url={0}, dbUser={1}, users={2}", jdbcUrl, username, users.size());
        long start = System.nanoTime();
        
        try (EntityManagerFactory emf = createEntityManagerFactory(jdbcUrl, username, password)) {
            LOG.log(INFO, "Schema created from entity mappings, persisting users...");
            seed(emf, users);
        }
        
        LOG.log(INFO, "Seeding finished in {0} ms", Duration.ofNanos(System.nanoTime() - start).toMillis());
    }
    
    // --- EMF construction ---
    
    /**
     * Configuration for the entity mappings with drop-and-create schema generation.
     * Exposed so callers can add or override properties before creating the EMF.
     */
    public static PersistenceConfiguration persistenceConfiguration(String jdbcUrl, String username, String password) {
        return new PersistenceConfiguration("dataverse-test-data")
            .managedClass(DataverseAuthenticatedUser.class)
            .managedClass(DataverseBuiltinUser.class)
            .property(PersistenceConfiguration.JDBC_URL, jdbcUrl)
            .property(PersistenceConfiguration.JDBC_USER, username)
            .property(PersistenceConfiguration.JDBC_PASSWORD, password)
            .property(PersistenceConfiguration.SCHEMAGEN_DATABASE_ACTION, "drop-and-create");
    }
    
    /**
     * Creates an EMF and (re)creates the schema. The caller is responsible for closing it.
     */
    public static EntityManagerFactory createEntityManagerFactory(String jdbcUrl, String username, String password) {
        return persistenceConfiguration(jdbcUrl, username, password).createEntityManagerFactory();
    }
    
    // --- Injected EMF ---
    
    /**
     * Persists the given users in a new transaction. Does not touch the schema.
     */
    public static void seed(EntityManagerFactory emf, List<TestUser> users) {
        emf.runInTransaction(em -> seed(em, users));
    }
    
    /**
     * Deletes all existing user rows, then persists the given users, all in a single transaction.
     * Useful for per-test isolation with a shared EMF.
     */
    public static void reset(EntityManagerFactory emf, List<TestUser> users) {
        emf.runInTransaction(em -> {
            deleteAll(em);
            seed(em, users);
        });
    }
    
    // --- Injected EM ---
    
    /**
     * Persists the given users using the caller's EntityManager.
     * MUST be called within an active transaction, which the caller is responsible for committing.
     */
    public static void seed(EntityManager em, List<TestUser> users) {
        users.forEach(user -> {
            var authenticatedUser = toAuthenticatedUser(user);
            var builtinUser = toBuiltinUser(user);
            em.persist(authenticatedUser);
            em.persist(builtinUser);
            LOG.log(INFO, "  + {0} (id={1}, email={2}, name={3} {4}, pwVersion={5}, hash={6})",
                user.username(), user.id(), user.email(), user.firstName(), user.lastName(),
                builtinUser.getPasswordEncryptionVersion(), builtinUser.getEncryptedPassword());
        });
        
        em.flush();
        long authCount = em.createQuery("select count(u) from DataverseAuthenticatedUser u", Long.class).getSingleResult();
        long builtinCount = em.createQuery("select count(u) from DataverseBuiltinUser u", Long.class).getSingleResult();
        LOG.log(INFO, "Read back: {0} row(s) in authenticateduser, {1} row(s) in builtinuser", authCount, builtinCount);
    }
    
    /**
     * Removes all user rows using the caller's EntityManager.
     * MUST be called within an active transaction, which the caller is responsible for committing.
     */
    public static void deleteAll(EntityManager em) {
        em.createQuery("delete from DataverseBuiltinUser").executeUpdate();
        em.createQuery("delete from DataverseAuthenticatedUser").executeUpdate();
    }
    
    private static DataverseAuthenticatedUser toAuthenticatedUser(TestUser user) {
        var entity = new DataverseAuthenticatedUser();
        entity.setId(user.id());
        entity.setUserIdentifier(user.username());
        entity.setEmail(user.email());
        entity.setFirstName(user.firstName());
        entity.setLastName(user.lastName());
        return entity;
    }
    
    private static DataverseBuiltinUser toBuiltinUser(TestUser user) {
        var entity = new DataverseBuiltinUser();
        entity.setId(user.id());
        entity.setUsername(user.username());
        entity.setEncryptedPassword(BCrypt.hashpw(user.password(), BCrypt.gensalt()));
        entity.setPasswordEncryptionVersion(BCRYPT_VERSION);
        return entity;
    }
}