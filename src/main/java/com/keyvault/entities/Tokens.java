package com.keyvault.entities;

import javax.persistence.*;
import java.io.Serial;
import java.io.Serializable;
import java.sql.Timestamp;
import java.util.Objects;

@Entity
public class Tokens implements Serializable {
    @Serial
    private static final long serialVersionUID = 6529685098267757692L;
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    @Column(name = "idT", nullable = false)
    private Integer idT;
    @Basic
    @Column(name = "value", nullable = false, length = 88)
    private String value;
    @Basic
    @Column(name = "date", nullable = false)
    private Timestamp date;
    @Basic
    @Column(name = "state", nullable = false)
    private byte state = 1;
    @Basic
    @Column(name = "isAuth", nullable = false)
    private byte isAuth = 1;
    @ManyToOne
    @JoinColumn(name = "idTu", referencedColumnName = "idU", nullable = false, updatable = false)
    private Users usersByIdTu;

    public Integer getIdT() {
        return idT;
    }

    public void setIdT(Integer idT) {
        this.idT = idT;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public Timestamp getDate() {
        return date;
    }

    public void setDate(Timestamp date) {
        this.date = date;
    }

    public byte getState() {
        return state;
    }

    public void setState(byte state) {
        this.state = state;
    }

    public byte getIsAuth() {
        return isAuth;
    }

    public void setIsAuth(byte isAuth) {
        this.isAuth = isAuth;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Tokens tokens = (Tokens) o;
        return state == tokens.state && Objects.equals(idT, tokens.idT)  && Objects.equals(value, tokens.value) && Objects.equals(date, tokens.date);
    }

    @Override
    public int hashCode() {
        return Objects.hash(idT, value, date, state);
    }

    public Users getUsersByIdTu() {
        return usersByIdTu;
    }

    public void setUsersByIdTu(Users usersByIdTu) {
        this.usersByIdTu = usersByIdTu;
    }
}
