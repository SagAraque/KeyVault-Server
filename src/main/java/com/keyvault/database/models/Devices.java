package com.keyvault.database.models;

import com.keyvault.PasswordController;
import io.ipgeolocation.api.Geolocation;
import io.ipgeolocation.api.GeolocationParams;
import io.ipgeolocation.api.IPGeolocationAPI;
import javax.persistence.*;
import java.io.IOException;
import java.io.Serial;
import java.io.Serializable;
import java.util.Date;
import java.util.Objects;

@Entity
public class Devices implements Serializable {
    @Serial
    private static final long serialVersionUID = 6529685098267757691L;
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    @Column(name = "idd", nullable = false)
    private Integer idD;
    @Basic
    @Column(name = "mac", nullable = false, length = 88)
    private String mac;
    @Basic
    @Column(name = "ip", nullable = false, length = 88)
    private String ip;
    @Basic
    @Column(name = "location", nullable = false, length = 128)
    private String location;
    @Basic
    @Column(name = "agent", nullable = false, length = 128)
    private String agent;
    @Basic
    @Temporal(TemporalType.DATE)
    @Column(name = "lastlogin", nullable = false)
    private Date lastLogin;
    @Basic
    @Column(name = "stated", nullable = false)
    private boolean stateD;
    @Basic
    @Column(name = "saltd", nullable = false, length = 88)
    private String saltD;
    @ManyToOne
    @JoinColumn(name = "idud", referencedColumnName = "idu", nullable = false)
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

    public boolean getStateD() {
        return stateD;
    }

    public void setStateD(boolean stateD) {
        this.stateD = stateD;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getAgent() {
        return agent;
    }

    public void setAgent(String agent) {
        this.agent = agent;
    }

    public Date getLastLogin() {
        return lastLogin;
    }

    public void setLastLogin(Date lastLogin) {
        this.lastLogin = lastLogin;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Devices devices = (Devices) o;
        return stateD == devices.stateD && Objects.equals(idD, devices.idD) && Objects.equals(mac, devices.mac) && Objects.equals(ip, devices.ip) && Objects.equals(location, devices.location) && Objects.equals(agent, devices.agent) && Objects.equals(lastLogin, devices.lastLogin) && Objects.equals(usersByIdUd, devices.usersByIdUd);
    }

    @Override
    public int hashCode() {
        return Objects.hash(idD, mac, ip, location, agent, lastLogin, stateD, usersByIdUd);
    }

    public Users getUsersByIdUd() {
        return usersByIdUd;
    }

    public void setUsersByIdUd(Users usersByIdUd) {
        this.usersByIdUd = usersByIdUd;
    }

    public void encrypt(PasswordController p) throws Exception
    {
        if(saltD == null)
            saltD = p.getSalt();

        location = p.encrypt(location, saltD);
        agent = p.encrypt(agent, saltD);
    }

    public void decrypt(PasswordController p) throws Exception
    {
        location = p.decrypt(location, saltD);
        agent = p.decrypt(agent, saltD);
    }

    public void geolocate() {
        IPGeolocationAPI api = new IPGeolocationAPI("b7e33bf7bf34483f9713c281f889d985");
        GeolocationParams geoParams = new GeolocationParams();

        if(this.ip.startsWith("127.0."))
        {
            geoParams.setIPAddress(this.ip.startsWith("127.0.") ? "213.37.28.35" : this.ip);
            geoParams.setFields("geo");
            Geolocation geolocation = api.getGeolocation(geoParams);

            if(geolocation.getStatus() == 200)
                this.location = geolocation.getCity() + ", " + geolocation.getCountryName();
            else
                this.location = "No location";
        }
        else
        {
            this.location = "No location";
        }
    }
}
