package com.keyvault.controllers;

import com.keyvault.PasswordController;
import com.keyvault.database.models.Devices;
import com.keyvault.database.HibernateUtils;
import com.keyvault.database.models.SessionToken;
import com.keyvault.database.models.Users;
import de.taimos.totp.TOTP;
import org.apache.commons.codec.binary.Base32;
import org.apache.commons.codec.binary.Hex;
import org.hibernate.*;
import javax.crypto.NoSuchPaddingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Date;

public class AuthController {
    private final PasswordController pc;
    private Users authUser = null;
    private SessionToken userToken = null;
    private String usersPepper;
    private String devicesPepper;
    private String plainEmail;
    private RedisTokensController tokensController;

    public AuthController(String usersPepper, String devicesPepper, String redisPassword) throws NoSuchPaddingException, NoSuchAlgorithmException {
        pc = new PasswordController();
        this.usersPepper = usersPepper;
        this.devicesPepper = devicesPepper;
        this.tokensController = new RedisTokensController(redisPassword);
    }

    public int authenticate(Users loginUser, Devices loginDevice)
    {
        Session session = null;

        try {
            session = HibernateUtils.getCurrentSession();
            Query q = session.createQuery("SELECT u, (SELECT d FROM Devices d WHERE d.usersByIdUd = u AND d.ip = :ip AND d.mac = :mac AND d.stateD = true) FROM Users u WHERE u.emailU = :email AND u.stateU = true");
            q.setParameter("email", pc.hashData(loginUser.getEmailU()));
            q.setParameter("ip", pc.hashData(loginDevice.getIp()));
            q.setParameter("mac", pc.hashData(loginDevice.getMac()));

            Object[] queryResult = (Object[]) q.uniqueResult();

            if(queryResult == null)
                return 101;

            Users user = (Users) queryResult[0];
            Devices device = (Devices) queryResult[1];

            if(user == null)
                return 101;

            pc.setToken(usersPepper);
            user.decrypt(pc);

            if(pc.hashData(loginUser.getPassU()).equals(user.getPassU()))
            {
                user.encrypt(pc);
                authUser = user;
                plainEmail = loginUser.getEmailU();

                if(device == null)
                    return 102;

                Transaction tx = session.beginTransaction();

                device.setLastLogin(new Date());

                session.update(device);
                tx.commit();
                session.close();
                return 200;
            }
            else
            {
                session.close();
                return 101;
            }

        } catch (Exception e) {
            e.printStackTrace();
            return 202;
        }finally {
            HibernateUtils.closeSession(session);
        }
    }

    public Users getAuthUser(){
        return authUser;
    }

    private boolean validateTOTP(String code){
        try {
            byte[] bytes = new Base32().decode(authUser.getKey2Fa());
            String hexKey = Hex.encodeHexString(bytes);
            Thread.sleep(2000);


            return TOTP.validate(hexKey, code);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public int validate2FA(String code){
        if (authUser.isTotpverified()){
            return validateTOTP(code) ? 200 : 103;
        }else{
            return tokensController.validateVerifyToken(code, authUser) ? 200 : 103;
        }
    }

    public void generateVerifyToken()
    {
        String code = tokensController.generateVerifyToken(authUser);
        new MailController(code, plainEmail).start();
    }

    public boolean checkSessionToken(SessionToken token)
    {
       boolean isAuth = tokensController.checkSessionToken(token);

       if(isAuth)
       {

           userToken = token;
           authUser = HibernateUtils.getCurrentSession().get(Users.class, token.getUser().getIdU());
       }


       return isAuth;
    }

    public void revalidateToken()
    {
        tokensController.revalidateToken(userToken);
    }

    public SessionToken generateToken() throws NoSuchAlgorithmException, NoSuchPaddingException {
        return tokensController.generateToken(authUser);
    }

    public int verify2FA(String code)
    {
        boolean isValid = validateTOTP(code);

        if(isValid)
        {
            Session session = HibernateUtils.getCurrentSession();
            Transaction tx = session.beginTransaction();
            authUser.setTotpverified(true);

            session.saveOrUpdate(authUser);
            tx.commit();

            HibernateUtils.closeSession(session);
        }

        return isValid ? 200 : 103;
    }

    public Users controlTOTP(){
        if(!authUser.isTotpverified()){
            SecureRandom num = new SecureRandom();
            byte[] bytes = new byte[20];
            num.nextBytes(bytes);
            String key = new Base32().encodeAsString(bytes);

            authUser.setKey2Fa(key.replaceAll("=", ""));
        }else{
            authUser.setKey2Fa(null);
        }

        authUser.setTotpverified(false);

        return authUser;

    }

    public String getQR(){
        String urlInfo = URLEncoder.encode("KeyVault", StandardCharsets.UTF_8).replace("+", "%20");
        String urlSecret = URLEncoder.encode(authUser.getKey2Fa(), StandardCharsets.UTF_8).replace("+", "%20");
        String urlIssuer = URLEncoder.encode("KeyVault", StandardCharsets.UTF_8).replace("+", "%20");

        return "otpauth://totp/" + urlInfo + "?secret=" + urlSecret + "&issuer=" + urlIssuer;
    }

    public int createUser(Users user, Devices device){
        Session session = null;
        Transaction tx = null;

        try{
            session = HibernateUtils.getCurrentSession();
            tx = session.beginTransaction();

            pc.setToken(usersPepper);
            user.setEmailU(pc.hashData(user.getEmailU()));
            user.setPassU(pc.hashData(user.getPassU()));
            user.setSaltU(pc.getSalt());
            user.encrypt(pc);

            pc.setToken(devicesPepper);

            device.geolocate();
            device.setIp(pc.hashData(device.getIp()));
            device.setMac(pc.hashData(device.getMac()));
            device.setLastLogin(new Date());
            device.encrypt(pc);

            Query q = session.createQuery("from Users u where u.emailU = :email and u.stateU = true");
            q.setParameter("email", user.getEmailU());

            if(q.list().isEmpty()){

                session.persist(user);

                device.setUsersByIdUd(user);

                session.persist(device);

                tx.commit();

                return 200;
            }

            return 104;

        } catch (Exception e){
            if(tx != null) tx.rollback();
            return 202;
        }finally {
            HibernateUtils.closeSession(session);
        }
    }
}
