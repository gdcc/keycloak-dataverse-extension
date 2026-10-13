package edu.harvard.iq.keycloak.auth.spi.models;

import java.util.Objects;

public class DataverseUser {

    private final DataverseAuthenticatedUser authenticatedUser;
    private final DataverseBuiltinUser builtinUser;

    public DataverseUser(DataverseAuthenticatedUser authenticatedUser, DataverseBuiltinUser builtinUser) {
        this.authenticatedUser = authenticatedUser;
        this.builtinUser = builtinUser;
    }

    public DataverseAuthenticatedUser getAuthenticatedUser() {
        return authenticatedUser;
    }

    public DataverseBuiltinUser getBuiltinUser() {
        return builtinUser;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o instanceof DataverseUser that)
            return Objects.equals(authenticatedUser, that.authenticatedUser) && Objects.equals(builtinUser, that.builtinUser);
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(authenticatedUser, builtinUser);
    }
}
