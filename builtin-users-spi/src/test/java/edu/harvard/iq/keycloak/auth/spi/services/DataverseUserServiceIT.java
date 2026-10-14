package edu.harvard.iq.keycloak.auth.spi.services;

import edu.harvard.iq.keycloak.auth.spi.fixtures.DataverseTestData;
import edu.harvard.iq.keycloak.auth.spi.fixtures.DataverseTestData.TestUser;
import edu.harvard.iq.keycloak.auth.spi.models.DataverseAuthenticatedUser;
import edu.harvard.iq.keycloak.auth.spi.models.DataverseBuiltinUser;
import edu.harvard.iq.keycloak.auth.spi.models.DataverseUser;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.keycloak.connections.jpa.JpaConnectionProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.storage.StorageId;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.util.List;

import static edu.harvard.iq.keycloak.auth.spi.fixtures.DataverseTestData.ALICE;
import static edu.harvard.iq.keycloak.auth.spi.services.DataverseUserService.PERSISTENCE_UNIT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Runs {@link DataverseUserService} against a live PostgreSQL. Keycloak is deliberately left out:
 * only the session/provider plumbing is mocked; queries, mappings and the DB itself are real.
 */
@Testcontainers
class DataverseUserServiceIT {
    
    /** Second user, so lookups must actually pick the right row. Local to this test to keep ALL_USERS stable. */
    private static final TestUser BOB = new TestUser(2, "bob", "hunter2", "bob@example.org", "Bob", "Builder");
    private static final List<TestUser> USERS = List.of(ALICE, BOB);
    
    private static final String POSTGRES_VERSION = System.getProperty("postgres.version", "invalidTag");
    @Container
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:" + POSTGRES_VERSION);
    
    private static EntityManagerFactory emf;
    
    private EntityManager sutEntityManager;
    private DataverseUserService sut;
    
    @BeforeAll
    static void createEntityManagerFactory() {
        // Container is already started by the extension; this creates the schema once per class
        emf = DataverseTestData.createEntityManagerFactory(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
    }
    
    @AfterAll
    static void closeEntityManagerFactory() {
        if (emf != null) {
            emf.close();
        }
    }
    
    @BeforeEach
    void setUp() {
        DataverseTestData.reset(emf, USERS);
        
        // Fresh EM, separate from the seeding one, so lookups really hit the DB and not a first-level cache
        sutEntityManager = emf.createEntityManager();
        
        KeycloakSession sessionMock = mock(KeycloakSession.class);
        JpaConnectionProvider jpaConnectionProviderMock = mock(JpaConnectionProvider.class);
        when(sessionMock.getProvider(JpaConnectionProvider.class, PERSISTENCE_UNIT)).thenReturn(jpaConnectionProviderMock);
        when(jpaConnectionProviderMock.getEntityManager()).thenReturn(sutEntityManager);
        
        sut = new DataverseUserService(sessionMock);
    }
    
    @AfterEach
    void tearDown() {
        if (sutEntityManager != null && sutEntityManager.isOpen()) {
            sutEntityManager.close();
        }
    }
    
    @Nested
    class GetUserById {
        @Test
        void getUserById_userExists() {
            DataverseUser user = sut.getUserById(String.valueOf(BOB.id()));
            
            assertNotNull(user);
            assertEquals(BOB.username(), user.getBuiltinUser().getUsername());
        }
        
        @Test
        void getUserById_withFederatedStorageId() {
            // The ID format Keycloak actually passes in: "f:<componentId>:<externalId>"
            String keycloakId = new StorageId("dataverse-provider", String.valueOf(ALICE.id())).getId();
            
            DataverseUser user = sut.getUserById(keycloakId);
            
            assertNotNull(user);
            assertEquals(ALICE.username(), user.getBuiltinUser().getUsername());
        }
        
        @Test
        void getUserById_userNotFound() {
            assertNull(sut.getUserById("999999"));
        }
        
        @Test
        void getUserById_builtinUserWithoutAuthenticatedUser() {
            removeAuthenticatedUser(BOB);
            assertNull(sut.getUserById(String.valueOf(BOB.id())));
        }
    }
    
    @Nested
    class GetUserByUsername {
        @Test
        void getUserByUsername_userExists() {
            DataverseUser user = sut.getUserByUsername(BOB.username());
            
            assertNotNull(user);
            assertEquals(BOB.username(), user.getBuiltinUser().getUsername());
        }
        
        @Test
        void getUserByUsername_userNotFound() {
            assertNull(sut.getUserByUsername("unknown"));
        }
        
        @Test
        void getUserByUsername_builtinUserWithoutAuthenticatedUser() {
            removeAuthenticatedUser(BOB);
            assertNull(sut.getUserByUsername(BOB.username()));
        }
    }
    
    @Nested
    class GetUserByEmail {
        @Test
        void getUserByEmail_userExists() {
            DataverseUser user = sut.getUserByEmail(BOB.email());
            
            assertNotNull(user);
            assertEquals(BOB.username(), user.getBuiltinUser().getUsername());
        }
        
        @Test
        void getUserByEmail_userNotFound() {
            assertNull(sut.getUserByEmail("unknown@example.org"));
        }
        
        @Test
        void getUserByEmail_authenticatedUserWithoutBuiltinUser() {
            removeBuiltinUser(BOB);
            assertNull(sut.getUserByEmail(BOB.email()));
        }
    }
    
    // --- helpers ---
    
    private static void removeAuthenticatedUser(TestUser user) {
        emf.runInTransaction(em -> em.remove(em.find(DataverseAuthenticatedUser.class, user.id())));
    }
    
    private static void removeBuiltinUser(TestUser user) {
        emf.runInTransaction(em -> em.remove(em.find(DataverseBuiltinUser.class, user.id())));
    }
}