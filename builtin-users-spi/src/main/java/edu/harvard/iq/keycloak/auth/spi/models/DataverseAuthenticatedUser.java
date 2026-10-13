package edu.harvard.iq.keycloak.auth.spi.models;

import jakarta.persistence.*;

import java.util.Objects;

@NamedQuery(name  = "DataverseAuthenticatedUser.findByEmail",
            query = "select au from DataverseAuthenticatedUser au WHERE LOWER(au.email)=LOWER(:email)")
@NamedQuery(name  = "DataverseAuthenticatedUser.findByIdentifier",
            query = "select au from DataverseAuthenticatedUser au WHERE LOWER(au.userIdentifier)=LOWER(:identifier)")
@Entity
@Table(name = "authenticateduser")
public class DataverseAuthenticatedUser {
    @Id
    private Integer id;
    private String email;
    private String lastName;
    private String firstName;
    private String userIdentifier;

    public void setId(Integer id) {
        this.id = id;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setUserIdentifier(String userIdentifier) {
        this.userIdentifier = userIdentifier;
    }

    public String getEmail() {
        return email;
    }

    public String getLastName() {
        return lastName;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getUserIdentifier() {
        return userIdentifier;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o instanceof DataverseAuthenticatedUser that)
            return Objects.equals(userIdentifier, that.userIdentifier) &&
                   Objects.equals(email, that.email) &&
                   Objects.equals(firstName, that.firstName) &&
                   Objects.equals(lastName, that.lastName);
        return false;
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(userIdentifier, email, firstName, lastName);
    }
}
