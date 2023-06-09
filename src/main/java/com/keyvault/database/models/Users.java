package com.keyvault.database.models;

import com.keyvault.PasswordController;
import javax.persistence.*;
import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

@Entity
public class Users implements Serializable {
    @Serial
    private static final long serialVersionUID = 6529685098267757690L;
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    @Column(name = "idu", nullable = false)
    private Integer idU;
    @Basic
    @Column(name = "emailu", nullable = false, length = 88)
    private String emailU;
    @Basic
    @Column(name = "passu", nullable = false, length = 210)
    private String passU;
    @Basic
    @Column(name = "saltu", nullable = false, length = 88)
    private String saltU;
    @Basic
    @Column(name = "key2fa", nullable = true, length = 88)
    private String key2Fa;
    @Basic
    @Column(name = "stateu", nullable = false)
    private boolean stateU = true;

    @Basic
    @Column(name = "totpverified", nullable = false)
    private boolean totpverified = false;

    public Integer getIdU() {
        return idU;
    }

    public void setIdU(Integer idU) {
        this.idU = idU;
    }

    public String getEmailU() {
        return emailU;
    }

    public void setEmailU(String emailU) {
        this.emailU = emailU;
    }

    public String getPassU() {
        return passU;
    }

    public void setPassU(String passU) {
        this.passU = passU;
    }

    public String getSaltU() {
        return saltU;
    }

    public void setSaltU(String saltU) {
        this.saltU = saltU;
    }

    public String getKey2Fa() {
        return key2Fa;
    }

    public void setKey2Fa(String key2Fa) {
        this.key2Fa = key2Fa;
    }

    public boolean getStateU() {
        return stateU;
    }

    public void setStateU(boolean stateU) {
        this.stateU = stateU;
    }

    public boolean isStateU() {
        return stateU;
    }

    public boolean isTotpverified() {
        return totpverified;
    }

    public void setTotpverified(boolean totpverified) {
        this.totpverified = totpverified;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Users users = (Users) o;
        return stateU == users.stateU && Objects.equals(idU, users.idU) && Objects.equals(emailU, users.emailU) && Objects.equals(passU, users.passU) && Objects.equals(saltU, users.saltU) && Objects.equals(key2Fa, users.key2Fa);
    }

    @Override
    public int hashCode() {
        return Objects.hash(idU, emailU, passU, saltU, key2Fa, stateU);
    }

    public void encrypt(PasswordController pc) throws Exception {
        passU = pc.encrypt(passU, saltU);
    }

    public void decrypt(PasswordController pc) throws Exception {
        passU = pc.decrypt(passU, saltU);
    }
}
