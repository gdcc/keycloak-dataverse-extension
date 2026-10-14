package edu.harvard.iq.keycloak.auth.spi.models;

import jakarta.persistence.*;

import java.util.Objects;

@NamedQuery(name  = "DataverseBuiltinUser.findByUsername",
            query = "SELECT u FROM DataverseBuiltinUser u WHERE LOWER(u.username)=LOWER(:username)")
@Entity
@Table(name = "builtinuser")
public class DataverseBuiltinUser {
    @Id
    private Integer id;

    private String username;

    private String encryptedPassword;

    private Integer passwordEncryptionVersion;

    public void setId(Integer id) {
        this.id = id;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public Integer getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public Integer getPasswordEncryptionVersion() {
        return passwordEncryptionVersion;
    }
    
    public void setPasswordEncryptionVersion(Integer passwordEncryptionVersion) {
        this.passwordEncryptionVersion = passwordEncryptionVersion;
    }

    public void setEncryptedPassword(String encryptedPassword) {
        this.encryptedPassword = encryptedPassword;
    }

    public String getEncryptedPassword() {
        return encryptedPassword;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o instanceof DataverseBuiltinUser that)
            return Objects.equals(username, that.username) && Objects.equals(encryptedPassword, that.encryptedPassword);
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(username, encryptedPassword);
    }
}
