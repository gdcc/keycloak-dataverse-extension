package edu.harvard.iq.keycloak.auth.spi.adapters;

import edu.harvard.iq.keycloak.auth.spi.models.DataverseUser;
import edu.harvard.iq.keycloak.auth.spi.providers.DataverseUserStorageProviderFactory;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.storage.StorageId;
import org.keycloak.storage.adapter.AbstractUserAdapterFederatedStorage;

import java.util.Objects;

public class DataverseUserAdapter extends AbstractUserAdapterFederatedStorage {

    protected DataverseUser dataverseUser;
    protected String keycloakId;

    private static final String ATTRIBUTE_NAME_IDP = "idp";

    public DataverseUserAdapter(KeycloakSession session, RealmModel realm, ComponentModel model, DataverseUser dataverseUser) {
        super(session, realm, model);
        this.dataverseUser = dataverseUser;
        keycloakId = StorageId.keycloakId(model, dataverseUser.getBuiltinUser().getId().toString());
        this.setSingleAttribute(ATTRIBUTE_NAME_IDP, DataverseUserStorageProviderFactory.PROVIDER_ID);
    }

    @Override
    public void setUsername(String s) {
        // Intentionally empty, as this adapter does not support setting the username
    }

    @Override
    public String getUsername() {
        return dataverseUser.getBuiltinUser().getUsername();
    }

    @Override
    public String getEmail() {
        return dataverseUser.getAuthenticatedUser().getEmail();
    }

    @Override
    public String getFirstName() {
        return dataverseUser.getAuthenticatedUser().getFirstName();
    }

    @Override
    public String getLastName() {
        return dataverseUser.getAuthenticatedUser().getLastName();
    }

    @Override
    public String getId() {
        return keycloakId;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o instanceof DataverseUserAdapter that)
            return keycloakId.equals(that.keycloakId) && Objects.equals(dataverseUser, that.dataverseUser);
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(keycloakId, dataverseUser);
    }
}
