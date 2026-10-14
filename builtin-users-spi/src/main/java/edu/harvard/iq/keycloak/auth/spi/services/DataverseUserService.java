package edu.harvard.iq.keycloak.auth.spi.services;

import edu.harvard.iq.keycloak.auth.spi.models.DataverseAuthenticatedUser;
import edu.harvard.iq.keycloak.auth.spi.models.DataverseBuiltinUser;
import edu.harvard.iq.keycloak.auth.spi.models.DataverseUser;
import jakarta.persistence.EntityManager;
import jakarta.persistence.FlushModeType;
import org.jboss.logging.Logger;
import org.keycloak.connections.jpa.JpaConnectionProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.storage.StorageId;

import java.util.List;
import java.util.function.Supplier;

public class DataverseUserService {

    private static final Logger logger = Logger.getLogger(DataverseUserService.class);
    // Keep the persistence unit name in sync with builtin-users-spi/src/main/resources/META-INF/persistence.xml!
    static final String PERSISTENCE_UNIT = "dataverse-pu";
    
    private final KeycloakSession session;
    
    public DataverseUserService(KeycloakSession session) {
        // Note: Do NOT obtain the EntityManager here: it would bind to Keycloak's outer transaction
        this.session = session;
    }
    
    public DataverseUser getUserById(String id) {
        return readOnly(() -> doGetUserById(id));
    }
    
    public DataverseUser getUserByUsername(String username) {
        return readOnly(() -> doGetUserByUsername(username));
    }
    
    public DataverseUser getUserByEmail(String email) {
        return readOnly(() -> doGetUserByEmail(email));
    }
    
    /**
     * All Dataverse DB access MUST go through here, so it runs in an isolated, read-only, rolled-back transaction.
     */
    private <T> T readOnly(Supplier<T> work) {
        return IsolatedReadOnlyTx.run(session, work);
    }
    
    /**
     * Only call from within {@link #readOnly(Supplier)}!
     */
    private EntityManager em() {
        EntityManager em = session.getProvider(JpaConnectionProvider.class, PERSISTENCE_UNIT).getEntityManager();
        // Standard JPA: no auto-flush before queries. Combined with the always-rollback tx, nothing is ever written
        em.setFlushMode(FlushModeType.COMMIT);
        return em;
    }
    
    /**
     * Only call from within {@link #readOnly(Supplier)} to attain transaction isolation!
     */
    private DataverseUser doGetUserById(String id) {
        logger.debugf("Fetching user by ID: %s", id);
        String persistenceId = StorageId.externalId(id);

        DataverseBuiltinUser builtinUser = em().find(DataverseBuiltinUser.class, persistenceId);
        if (builtinUser == null) {
            logger.debugf("Builtin user not found for external ID: %s", persistenceId);
            return null;
        }

        String username = builtinUser.getUsername();
        DataverseAuthenticatedUser authenticatedUser = getAuthenticatedUserByUsername(username);
        if (authenticatedUser == null) {
            logger.debugf("Authenticated user not found by username: %s", username);
            return null;
        }

        return new DataverseUser(authenticatedUser, builtinUser);
    }
    
    /**
     * Only call from within {@link #readOnly(Supplier)} to attain transaction isolation!
     */
    private DataverseUser doGetUserByUsername(String username) {
        logger.debugf("Fetching user by username: %s", username);
        List<DataverseBuiltinUser> users = em().createNamedQuery("DataverseBuiltinUser.findByUsername", DataverseBuiltinUser.class)
                .setParameter("username", username)
                .getResultList();

        if (users.isEmpty()) {
            logger.debugf("Builtin user not found by username: %s", username);
            return null;
        }

        DataverseAuthenticatedUser authenticatedUser = getAuthenticatedUserByUsername(username);
        if (authenticatedUser == null) {
            logger.debugf("Authenticated user not found by username: %s", username);
            return null;
        }

        return new DataverseUser(authenticatedUser, users.get(0));
    }
    
    /**
     * Only call from within {@link #readOnly(Supplier)} to attain transaction isolation!
     */
    private DataverseUser doGetUserByEmail(String email) {
        logger.debugf("Fetching user by email: %s", email);
        List<DataverseAuthenticatedUser> authUsers = em().createNamedQuery("DataverseAuthenticatedUser.findByEmail", DataverseAuthenticatedUser.class)
                .setParameter("email", email)
                .getResultList();

        if (authUsers.isEmpty()) {
            logger.debugf("Authenticated user not found by email: %s", email);
            return null;
        }

        String username = authUsers.get(0).getUserIdentifier();
        List<DataverseBuiltinUser> builtinUsers = em().createNamedQuery("DataverseBuiltinUser.findByUsername", DataverseBuiltinUser.class)
                .setParameter("username", username)
                .getResultList();

        if (builtinUsers.isEmpty()) {
            logger.debugf("Builtin user not found by username: %s", username);
            return null;
        }

        return new DataverseUser(authUsers.get(0), builtinUsers.get(0));
    }

    /**
     * Retrieves an authenticated user from Dataverse by username.
     * Only call from within {@link #readOnly(Supplier)} to attain transaction isolation!
     *
     * @param username The username to look up.
     * @return The authenticated user or null if not found.
     */
    private DataverseAuthenticatedUser getAuthenticatedUserByUsername(String username) {
        try {
            return em().createNamedQuery("DataverseAuthenticatedUser.findByIdentifier", DataverseAuthenticatedUser.class)
                    .setParameter("identifier", username)
                    .getSingleResult();
        } catch (Exception e) {
            logger.debugf("Could not find authenticated user by username: %s", username);
            return null;
        }
    }
}
