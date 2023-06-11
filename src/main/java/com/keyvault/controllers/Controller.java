package com.keyvault.controllers;

import com.keyvault.PasswordController;
import com.keyvault.database.HibernateUtils;
import com.keyvault.database.models.Devices;
import com.keyvault.database.models.Items;
import com.keyvault.database.models.Users;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.query.Query;
import javax.crypto.NoSuchPaddingException;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;


public class Controller {
    private final PasswordController pc;
    private final String itemsPepper, devicesPepper;

    public Controller(String itemsPepper, String devicesPepper) throws NoSuchPaddingException, NoSuchAlgorithmException
    {
        this.pc = new PasswordController();
        this.itemsPepper = itemsPepper;
        this.devicesPepper = devicesPepper;
    }

    public int saveImage(BufferedImage bufferedImage, Users user)
    {
        try {
            BufferedImage resized = bufferedImage;

            if(bufferedImage.getHeight() != 60 || bufferedImage.getWidth() != 60)
            {
                Image result = bufferedImage.getScaledInstance(60, 60, Image.SCALE_DEFAULT);
                resized = new BufferedImage(60, 60, BufferedImage.TYPE_INT_RGB);
                resized.getGraphics().drawImage(result, 0, 0, null);
            }

            File savedImage = new File("/home/keyvault/img/" + user.getIdU() + ".png");
            ImageIO.write(resized, "png", savedImage);

            return 200;
        } catch (IOException e) {
            return 202;
        }
    }

    public ByteArrayOutputStream getImage(Users user)
    {
        try
        {
            File image = new File("/home/keyvault/img/" + user.getIdU() + ".png");
            ByteArrayOutputStream imageBytes = new ByteArrayOutputStream();
            ImageIO.write(ImageIO.read(image), "png", imageBytes);

            return imageBytes;
        }
        catch (IOException | NullPointerException e)
        {
            return null;
        }
    }

    public List<Items> getUserItems(Users user)
    {
        Session session = null;
        List<Items> list = new ArrayList<>();
        ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() * 2);

        try
        {
            session = HibernateUtils.getCurrentSession();

            Query items = session.createQuery("select i from Items i join i.usersByIdUi u where u.idU = :id").setReadOnly(true);
            items.setParameter("id", user.getIdU());

            list = items.list();

            pc.setToken(itemsPepper);

            list.forEach(item -> {
                executorService.submit(() -> {
                    item.setUsersByIdUi(null);

                    try {
                        item.decrypt(pc);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            });

            executorService.shutdown();
            executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        }
        catch (Exception ignored)
        {

        }
        finally
        {
            HibernateUtils.closeSession(session);
        }

        return list;
    }

    public List<Devices> getUsersDevices(Users user) {
        Session session = null;
        List<Devices> list = new ArrayList<>();
        ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() * 2);

        try
        {
            session = HibernateUtils.getCurrentSession();

            Query devices = session.createQuery("select d from Devices d join d.usersByIdUd u where u.idU = :id").setReadOnly(true);
            devices.setParameter("id", user.getIdU());

            list = devices.list();

            pc.setToken(devicesPepper);

            list.forEach(device -> {
                executorService.submit(() -> {
                    try {
                        device.decrypt(pc);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });
            });

            executorService.shutdown();
            executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);

        }
        catch (Exception ignored)
        {

        }
        finally
        {
            HibernateUtils.closeSession(session);
        }

        return list;
    }

    public int createItem(Items item){
        Session session = null;
        Transaction tx = null;

        try{
            session = HibernateUtils.getCurrentSession();
            tx = session.beginTransaction();

            pc.setToken(itemsPepper);

            item.encrypt(pc);

            session.persist(item);
            session.persist(item.getPasswordsByIdI() == null ? item.getNotesByIdI() : item.getPasswordsByIdI());

            tx.commit();
        }catch (Exception e){
            if(tx != null) tx.rollback();

            return -1;
        }finally {
            HibernateUtils.closeSession(session);
        }

        return item.getIdI();
    }

    public int modifyItem(Items item, Users user){
        if(Objects.equals(item.getIdUi(), user.getIdU())){
            Session session = null;
            Transaction tx = null;

            try{
                session = HibernateUtils.getCurrentSession();
                tx = session.beginTransaction();

                Query q = session.createQuery("SELECT i.saltI FROM Items i WHERE i.id = :idI AND i.idUi = :idU");
                q.setParameter("idI", item.getIdI());
                q.setParameter("idU", user.getIdU());

                pc.setToken(itemsPepper);

                item.setSaltI((String) q.uniqueResult());
                item.setModification(new Timestamp(System.currentTimeMillis()));
                item.encrypt(pc);

                session.update(item);
                tx.commit();
            }catch (Exception e){
                if(tx != null) tx.rollback();
                return 202;
            }finally {
                HibernateUtils.closeSession(session);
            }

            return 200;
        }else{
            return 201;
        }

    }

    public int deleteItem(Items item, Users user){
        if(Objects.equals(item.getIdUi(), user.getIdU())){
            Session session = null;
            Transaction tx = null;
            item.setSaltI("");

            try{
                session = HibernateUtils.getCurrentSession();
                tx = session.beginTransaction();

                session.delete(item);
                tx.commit();
                return 200;
            }catch (HibernateException e){
                if(tx != null) tx.rollback();
                return 202;
            }finally {
                HibernateUtils.closeSession(session);
            }

        }else{
            return 201;
        }
    }

    public void addDevice(Devices device, Users user){
        Session session = null;
        Transaction tx = null;

        try{
            session = HibernateUtils.getCurrentSession();
            tx = session.beginTransaction();

            pc.setToken(devicesPepper);
            String hashIp = pc.hashData(device.getIp());
            String hashMac = pc.hashData(device.getMac());

            Query query = session.createQuery("From Devices d where d.usersByIdUd.id = :idU and d.ip = :ip and d.mac = :mac");
            query.setParameter("idU", user.getIdU());
            query.setParameter("ip", hashIp);
            query.setParameter("mac", hashMac);

            Devices queryDevice = (Devices) query.uniqueResult();

            if(queryDevice != null)
            {
                queryDevice.setLastLogin(new Date());
                queryDevice.setStateD(true);

                session.saveOrUpdate(queryDevice);
            }
            else
            {
                device.setLastLogin(new Date());
                device.geolocate();
                device.setIp(hashIp);
                device.setMac(hashMac);
                device.setUsersByIdUd(user);

                device.encrypt(pc);

                session.persist(device);
            }

            tx.commit();

        }catch (Exception e){
            if(tx != null) tx.rollback();
        }finally {
            HibernateUtils.closeSession(session);
        }
    }

    public int clearDevice(Users user){
        Session session = null;
        Transaction tx = null;

        try{
            session = HibernateUtils.getCurrentSession();
            tx = session.beginTransaction();

            Query q = session.createQuery("UPDATE Devices d set d.stateD = false where d.usersByIdUd.idU = :user and d.stateD = true");
            q.setParameter("user", user.getIdU());
            q.executeUpdate();

            tx.commit();

            return 200;
        }catch (HibernateException e){
            if(tx != null) tx.rollback();
            return 202;
        }finally {
            HibernateUtils.closeSession(session);
        }
    }

    public int updateUser(Users user){
        Session session = null;
        Transaction tx = null;

        try{
            session = HibernateUtils.getCurrentSession();
            tx = session.beginTransaction();

            session.saveOrUpdate(user);
            tx.commit();

            return 200;

        }catch (HibernateException e){
            if(tx != null) tx.rollback();
            return 202;
        }finally {
            HibernateUtils.closeSession(session);
        }
    }

    public int deleteUserAccount(Users user){
        Session session = null;
        Transaction tx = null;

        try{
            session = HibernateUtils.getCurrentSession();
            tx = session.beginTransaction();
            Query query = session.createQuery("DELETE Items i WHERE i.idUi = :id");
            query.setParameter("id", user.getIdU());

            query.executeUpdate();
            session.saveOrUpdate(user);
            tx.commit();

            return 200;

        }catch (HibernateException e){
            if(tx != null)
                tx.rollback();
            return 202;
        }finally {
            HibernateUtils.closeSession(session);
        }
    }
}
