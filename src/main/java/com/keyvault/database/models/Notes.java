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
public class Notes implements Serializable {
    @Serial
    private static final long serialVersionUID = 6529685098267757694L;
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    @Column(name = "idn", nullable = false)
    private Integer idN;
    @Basic
    @Column(name = "content", nullable = false, length = -1)
    private String content;
    @OneToOne
    @JoinColumn(name = "idin", referencedColumnName = "idi", nullable = false)
    private Items itemsByIdIn;

    public Integer getIdN() {
        return idN;
    }

    public void setIdN(Integer idN) {
        this.idN = idN;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Notes notes = (Notes) o;
        return Objects.equals(idN, notes.idN)  && Objects.equals(content, notes.content);
    }

    @Override
    public int hashCode() {
        return Objects.hash(idN, content);
    }

    public Items getItemsByIdIn() {
        return itemsByIdIn;
    }

    public void setItemsByIdIn(Items itemsByIdIn) {
        this.itemsByIdIn = itemsByIdIn;
    }

    public void encrypt(PasswordController pc) throws Exception {
        this.content = pc.encrypt(this.content, getItemsByIdIn().getSaltI());
    }

    public void decrypt(PasswordController pc) throws Exception {
        this.content = pc.decrypt(this.content, getItemsByIdIn().getSaltI());
    }
}
