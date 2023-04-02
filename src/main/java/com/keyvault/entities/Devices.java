package com.keyvault.entities;

import javax.persistence.*;
import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

@Entity
public class Devices implements Serializable {
    @Serial
    private static final long serialVersionUID = 6529685098267757691L;
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    @Column(name = "idD", nullable = false)
    private Integer idD;
    @Basic
    @Column(name = "mac", nullable = false, length = 88)
    private String mac;
    @Basic
    @Column(name = "ip", nullable = false, length = 88)
    private String ip;
    @Basic
    @Column(name = "stateD", nullable = false)
    private byte stateD;
    @ManyToOne
    @JoinColumn(name = "idUd", referencedColumnName = "idU", nullable = false)
    private Users usersByIdUd;

    public Integer getIdD() {
        return idD;
    }

    public void setIdD(Integer idD) {
        this.idD = idD;
    }

    public String getMac() {
        return mac;
    }

    public void setMac(String mac) {
        this.mac = mac;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public byte getStateD() {
        return stateD;
    }

    public void setStateD(byte stateD) {
        this.stateD = stateD;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Devices devices = (Devices) o;
        return stateD == devices.stateD && Objects.equals(idD, devices.idD) && Objects.equals(mac, devices.mac) && Objects.equals(ip, devices.ip);
    }

    @Override
    public int hashCode() {
        return Objects.hash(idD, mac, ip, stateD);
    }

    public Users getUsersByIdUd() {
        return usersByIdUd;
    }

    public void setUsersByIdUd(Users usersByIdUd) {
        this.usersByIdUd = usersByIdUd;
    }
}
