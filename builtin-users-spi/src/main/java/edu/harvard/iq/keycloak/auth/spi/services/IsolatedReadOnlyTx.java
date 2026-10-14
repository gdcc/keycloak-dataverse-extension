package edu.harvard.iq.keycloak.auth.spi.services;

import jakarta.transaction.Status;
import jakarta.transaction.Transaction;
import jakarta.transaction.TransactionManager;
import org.keycloak.models.KeycloakSession;
import org.keycloak.transaction.JtaTransactionManagerLookup;

import java.util.function.Supplier;

/**
 * Runs read-only work against the Dataverse DB in an isolated JTA transaction,
 * so it never participates in Keycloak's own (2PC) transaction.
 */
final class IsolatedReadOnlyTx {
    
    private IsolatedReadOnlyTx() {
        // Utility class not to be instantiated
    }
    
    static <T> T run(KeycloakSession session, Supplier<T> work) {
        JtaTransactionManagerLookup lookup = session.getProvider(JtaTransactionManagerLookup.class);
        TransactionManager tm = lookup != null ? lookup.getTransactionManager() : null;
        if (tm == null) {
            return work.get(); // No JTA available, nothing to isolate from
        }
        
        Transaction outer = null;
        try {
            if (tm.getStatus() != Status.STATUS_NO_TRANSACTION) {
                outer = tm.suspend();
            }
            tm.begin();
            try {
                return work.get();
            } finally {
                // Read-only: always roll back, nothing should ever be written
                tm.rollback();
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("Dataverse read transaction failed", e);
        } finally {
            if (outer != null) {
                try {
                    tm.resume(outer);
                } catch (Exception e) {
                    throw new IllegalStateException("Could not resume Keycloak transaction", e);
                }
            }
        }
    }
}
