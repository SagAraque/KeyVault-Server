package com.keyvault.controllers;

import com.keyvault.PasswordController;
import com.keyvault.database.models.Devices;
import com.keyvault.database.HibernateUtils;
import com.keyvault.database.models.Tokens;
import com.keyvault.database.models.Users;
import de.taimos.totp.TOTP;
import org.apache.commons.codec.binary.Base32;
import org.apache.commons.codec.binary.Hex;
import org.hibernate.*;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.sql.Timestamp;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public class AuthController {
    private final SessionFactory sf;
    private final PasswordController pc;
    private Users authUser = null;
    private Tokens userToken = null;
    private String usersPepper, devicesPepper, plainEmail;

    public AuthController(String usersPepper, String devicesPepper){
        sf = HibernateUtils.getSessionFactory();
        pc = new PasswordController();
        this.usersPepper = usersPepper;
        this.devicesPepper = devicesPepper;
    }

    public int authenticate(Users loginUser, Devices loginDevice){
        try {
            if(!checkUser(loginUser.getEmailU(), loginUser.getPassU()))
                return 101;
            if(!checkDevice(loginUser.getEmailU(), loginDevice.getIp(), loginDevice.getMac()))
                return 102;

            return 200;
        } catch (Exception e) {
            e.printStackTrace();
            return 202;
        }
    }

    public boolean checkUser(String email, String password) throws Exception {
        Session session = sf.openSession();

        Query q = session.createQuery("from Users u where u.emailU = :email and u.stateU = 1");

        q.setParameter("email", pc.hashData(email));

        List<Users> userList = q.list();

        if(userList.isEmpty())
            return false;

        session.close();

        pc.setToken(usersPepper);

        Users user = userList.get(0);
        System.out.println("Init check decrypt");
        user.decrypt(pc);

        if(pc.hashData(password).equals(user.getPassU())){
            authUser = user;
            plainEmail = email;
            System.out.println("Init check encrypt");
            authUser.encrypt(pc);
            return true;
        }else{
            return false;
        }
    }

    public boolean checkDevice(String email, String ip, String mac) throws NoSuchAlgorithmException {
        Session session = sf.openSession();

        Query q = session.createQuery("from Devices d where d.ip = :ip and d.mac = :mac and d.usersByIdUd.emailU = :email and d.stateD = 1");

        q.setParameter("email", pc.hashData(email));
        q.setParameter("ip", pc.hashData(ip));
        q.setParameter("mac", pc.hashData(mac));

        Devices device = (Devices) q.uniqueResult();

        if(device != null)
        {
            Transaction tx = session.beginTransaction();
            device.setLastLogin(new Date());
            session.update(device);
            tx.commit();
        }

        session.close();

        return device != null;
    }

    public Users getAuthUser(){
        return authUser;
    }

    public void generateAuthCode(){
        Session session = sf.openSession();
        Transaction tx = session.beginTransaction();

        int authNum = new Random().nextInt(100000, 999999);

        Tokens token = new Tokens();
        token.setIsAuth((byte) 0);
        token.setDate(new Timestamp(System.currentTimeMillis()));
        token.setState((byte) 1);
        token.setUsersByIdTu(authUser);
        token.setValue(String.valueOf(authNum));

        session.persist(token);
        tx.commit();

        ///new Mailer(authNum, plainEmail).start();
        System.out.println(authNum);
    }

    public boolean checkSessionToken(Tokens token){
        Session session = sf.openSession();
        Query<Tokens> q = session.createQuery("from Tokens t where t.state = 1 and t.usersByIdTu.idU = :user and t.value = :token");
        q.setParameter("user", token.getUsersByIdTu().getIdU());
        q.setParameter("token", token.getValue());

        Tokens serverToken = q.uniqueResult();
        long diff = System.currentTimeMillis() - token.getDate().getTime();

        if(serverToken != null){
            if(diff < 600000){
                authUser = serverToken.getUsersByIdTu();
                userToken = serverToken;
            }else{
                Transaction tx = session.beginTransaction();
                serverToken.setState((byte) 0);
                session.update(serverToken);
                tx.commit();
            }
        }

        session.close();
        return serverToken != null && diff < 600000;
    }

    public Tokens generateToken(){

        try {
            Random random = ThreadLocalRandom.current();
            byte[] bytes = new byte[64];
            random.nextBytes(bytes);

            String token = pc.hashData(authUser.getIdU() + System.currentTimeMillis() + new String(bytes));

            Session session = sf.openSession();
            Transaction tx = session.beginTransaction();

            Query q = session.createQuery("UPDATE FROM Tokens t SET t.state = 0 WHERE t.usersByIdTu.idU = :user AND t.state = 1");
            q.setParameter("user", authUser.getIdU());
            q.executeUpdate();

            Tokens newToken = new Tokens();
            newToken.setUsersByIdTu(authUser);
            newToken.setValue(token);
            newToken.setDate(new Timestamp(System.currentTimeMillis()));

            session.persist(newToken);

            tx.commit();

            userToken = newToken;

            return newToken;
        } catch (NoSuchAlgorithmException e) {
            return null;
        }
    }

    public Tokens revalidateToken(){
        if(userToken != null){
            Session session = sf.openSession();
            Transaction tx = session.beginTransaction();
            session.refresh(authUser);

            userToken.setDate(new Timestamp(System.currentTimeMillis()));
            userToken.setUsersByIdTu(authUser);

            session.saveOrUpdate(userToken);
            tx.commit();
        }

        return userToken;

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

    private boolean checkAuthNum(String num){
        Session session = sf.openSession();
        Query q = session.createQuery("FROM Tokens t where t.usersByIdTu.emailU = :user and t.state = 1 and t.isAuth = 0 order by t.date desc");
        q.setParameter("user", authUser.getEmailU());
        q.setMaxResults(1);

        Tokens token = (Tokens) q.uniqueResult();

        if(token != null && num.equals(token.getValue())){
            Transaction tx = session.beginTransaction();

            token.setState((byte) 0);
            session.update(token);
            tx.commit();

            return true;
        }else{
            return false;
        }
    }

    public int validate2FA(String code){
        if (authUser.isHas2fa()){
            return validateTOTP(code) ? 200 : 103;
        }else{
            return checkAuthNum(code) ? 200 : 103;
        }
    }

    public Users controlTOTP(){
        if(!authUser.isHas2fa()){
            SecureRandom num = new SecureRandom();
            byte[] bytes = new byte[20];
            num.nextBytes(bytes);
            String key = new Base32().encodeAsString(bytes);

            authUser.setKey2Fa(key.replaceAll("=", ""));
            authUser.setHas2fa(true);
        }else{
            authUser.setKey2Fa(null);
            authUser.setHas2fa(false);
        }

        return authUser;

    }

    public String getQR(){
        String urlInfo = URLEncoder.encode("KeyVault", StandardCharsets.UTF_8).replace("+", "%20");
        String urlSecret = URLEncoder.encode(authUser.getKey2Fa(), StandardCharsets.UTF_8).replace("+", "%20");
        String urlIssuer = URLEncoder.encode("KeyVault", StandardCharsets.UTF_8).replace("+", "%20");

        return "otpauth://totp/" + urlInfo + "?secret=" + urlSecret + "&issuer=" + urlIssuer;
    }

    public int createUser(Users user, Devices device){
        Session session = sf.openSession();
        Transaction tx = session.beginTransaction();

        try(session){
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

            Query q = session.createQuery("from Users u where u.emailU = :email and u.stateU = 1");
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
            tx.rollback();
            return 202;
        }
    }
}
