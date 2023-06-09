package com.keyvault.database.models;

import com.keyvault.PasswordController;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import javax.persistence.*;
import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;
@Entity
@Cacheable
@org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class Passwords implements Serializable {
    @Serial
    private static final long serialVersionUID = 6529685098267757695L;
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    @Column(name = "idp", nullable = false)
    private Integer idP;
    @Basic
    @Column(name = "emailp", nullable = false, length = 210)
    private String emailP;
    @Basic
    @Column(name = "passp", nullable = false, length = 210)
    private String passP;
    @Basic
    @Column(name = "url", nullable = false, length = -1)
    private String url;
    @OneToOne
    @JoinColumn(name = "idip", referencedColumnName = "idi", nullable = false)
    private Items itemsByIdIp;

    public Integer getIdP() {
        return idP;
    }

    public void setIdP(Integer idP) {
        this.idP = idP;
    }

    public String getEmailP() {
        return emailP;
    }

    public void setEmailP(String emailP) {
        this.emailP = emailP;
    }

    public String getPassP() {
        return passP;
    }

    public void setPassP(String passP) {
        this.passP = passP;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Passwords passwords = (Passwords) o;
        return Objects.equals(idP, passwords.idP)  && Objects.equals(emailP, passwords.emailP) && Objects.equals(passP, passwords.passP) && Objects.equals(url, passwords.url);
    }

    @Override
    public int hashCode() {
        return Objects.hash(idP, emailP, passP, url);
    }

    public Items getItemsByIdIp() {
        return itemsByIdIp;
    }

    public void setItemsByIdIp(Items itemsByIdIp) {
        this.itemsByIdIp = itemsByIdIp;
    }

    public void encrypt(PasswordController pc) throws Exception {
        if(this.url != null)
            this.url = pc.encrypt(this.url, getItemsByIdIp().getSaltI());
        this.emailP = pc.encrypt(this.emailP, getItemsByIdIp().getSaltI());
        this.passP = pc.encrypt(this.passP, getItemsByIdIp().getSaltI());
    }

    public void decrypt(PasswordController pc) throws Exception {
        if(this.url != null)
            this.url = pc.decrypt(this.url, getItemsByIdIp().getSaltI());

        this.emailP = pc.decrypt(this.emailP, getItemsByIdIp().getSaltI());
        this.passP = pc.decrypt(this.passP, getItemsByIdIp().getSaltI());
    }
}
