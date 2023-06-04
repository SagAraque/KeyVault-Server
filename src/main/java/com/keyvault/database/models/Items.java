package com.keyvault.database.models;

import com.keyvault.PasswordController;
import javax.persistence.*;
import java.io.Serial;
import java.io.Serializable;
import java.sql.Timestamp;
import java.util.Objects;

@Entity
public class Items implements Serializable {
    @Serial
    private static final long serialVersionUID = 6529685098267757693L;
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    @Column(name = "idi", nullable = false)
    private Integer idI;
    @Basic
    @Column(name = "idui", nullable = false)
    private Integer idUi;
    @Basic
    @Column(name = "name", nullable = false, length = 88)
    private String name;
    @Basic
    @Column(name = "observations", nullable = false, length = -1)
    private String observations;
    @Basic
    @Column(name = "modification", nullable = false)
    private Timestamp modification;
    @Basic
    @Column(name = "fav", nullable = false)
    private boolean fav;
    @Basic
    @Column(name = "salti", nullable = false, length = 88)
    private String saltI;
    @Basic
    @Column(name = "color", nullable = false, length = 7)
    private String color;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idui", referencedColumnName = "idu", nullable = false, insertable = false, updatable = false)
    private Users usersByIdUi;
    @OneToOne(mappedBy = "itemsByIdIn")
    private Notes notesByIdI;
    @OneToOne(mappedBy = "itemsByIdIp")
    private Passwords passwordsByIdI;

    public Integer getIdI() {
        return idI;
    }

    public void setIdI(Integer idI) {
        this.idI = idI;
    }

    public Integer getIdUi() {
        return idUi;
    }

    public void setIdUi(Integer idUi) {
        this.idUi = idUi;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getObservations() {
        return observations;
    }

    public void setObservations(String observations) {
        this.observations = observations;
    }

    public Timestamp getModification() {
        return modification;
    }

    public void setModification(Timestamp modification) {
        this.modification = modification;
    }

    public boolean getFav() {
        return fav;
    }

    public void setFav(boolean fav) {
        this.fav = fav;
    }

    public String getSaltI() {
        return saltI;
    }

    public void setSaltI(String saltI) {
        this.saltI = saltI;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Items items = (Items) o;
        return fav == items.fav && Objects.equals(idI, items.idI) && Objects.equals(idUi, items.idUi) && Objects.equals(name, items.name) && Objects.equals(observations, items.observations) && Objects.equals(modification, items.modification) && Objects.equals(saltI, items.saltI) && Objects.equals(color, items.color) && Objects.equals(usersByIdUi, items.usersByIdUi) && Objects.equals(notesByIdI, items.notesByIdI) && Objects.equals(passwordsByIdI, items.passwordsByIdI);
    }

    @Override
    public int hashCode() {
        return Objects.hash(idI, idUi, name, observations, modification, fav, saltI, color, usersByIdUi, notesByIdI, passwordsByIdI);
    }

    public Users getUsersByIdUi() {
        return usersByIdUi;
    }

    public void setUsersByIdUi(Users usersByIdUi) {
        this.usersByIdUi = usersByIdUi;
    }

    public Notes getNotesByIdI() {
        return notesByIdI;
    }

    public void setNotesByIdI(Notes notesByIdI) {
        this.notesByIdI = notesByIdI;
    }

    public Passwords getPasswordsByIdI() {
        return passwordsByIdI;
    }

    public void setPasswordsByIdI(Passwords passwordsByIdI) {
        this.passwordsByIdI = passwordsByIdI;
    }

    public void encrypt(PasswordController pc) throws Exception {
        if(this.saltI == null)
            this.saltI = pc.getSalt();

        this.name = pc.encrypt(this.name, this.saltI);

        if(this.observations != null)
            this.observations = pc.encrypt(this.observations, this.saltI);

        if(notesByIdI != null){
            notesByIdI.encrypt(pc);
        }else{
            passwordsByIdI.encrypt(pc);
        }
    }

    public void decrypt(PasswordController pc) throws Exception {
        this.name = pc.decrypt(this.name, this.saltI);

        if(this.observations != null)
            this.observations = pc.decrypt(this.observations, this.saltI);
        if(notesByIdI != null){
            notesByIdI.decrypt(pc);
        }else if(passwordsByIdI != null){
            passwordsByIdI.decrypt(pc);
        }
    }
}
