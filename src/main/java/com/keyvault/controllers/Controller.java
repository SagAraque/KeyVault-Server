package com.keyvault.controllers;

import com.keyvault.PasswordController;
import com.keyvault.database.HibernateUtils;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.query.Query;
import java.sql.Timestamp;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;


public class Controller {
    private final SessionFactory sf;
    private final PasswordController pc;
    private final String itemsPepper, devicesPepper;
    private ExecutorService executor;

    public Controller(String itemsPepper, String devicesPepper){
        sf = HibernateUtils.getSessionFactory();
        pc = new PasswordController();
        this.itemsPepper = itemsPepper;
        this.devicesPepper = devicesPepper;
        executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
    }

    public List<Items> getUserItems(Users user) throws Exception {
        Session session = sf.openSession();
        Query<Items> items = session.createQuery("from Items i where i.idUi = :id");
        items.setParameter("id", user.getIdU());

        List<Items> list = items.list();

        System.out.println(java.time.LocalTime.now() + " Init decrypt");

        pc.setToken(itemsPepper);

        for (Items item : list)
        {
            executor.submit(() -> {
                try {
                    item.decrypt(pc);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
        }

        executor.shutdown();
        executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);

        System.out.println(java.time.LocalTime.now() + " decrypted");
        session.close();

        return list;
    }

    public List<Devices> getUsersDevices(Users user) throws InterruptedException {
        Session session = sf.openSession();
        Query<Devices> devices = session.createQuery("from Devices d where d.usersByIdUd.id = :id");
        devices.setParameter("id", user.getIdU());

        List<Devices> list = devices.list();

        pc.setToken(devicesPepper);

        for (Devices device : list)
        {
            executor.submit(() -> {
                try {
                    device.decrypt(pc);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
        }

        executor.shutdown();
        executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);

        session.close();

        return list;
    }

    public int createItem(Items item, Users user){
        Session session = sf.openSession();
        Transaction tx = null;

        try (session) {
            pc.setToken(itemsPepper);

            item.encrypt(pc);

            tx = session.beginTransaction();

            session.persist(item);
            session.persist(item.getPasswordsByIdI() == null ? item.getNotesByIdI() : item.getPasswordsByIdI());

            tx.commit();
        } catch (Exception e) {
            if(tx != null) tx.rollback();
            return 202;
        }

        return 200;
    }

    public int modifyItem(Items item, Users user){
        if(Objects.equals(item.getIdUi(), user.getIdU())){
            Session session = sf.openSession();
            Transaction tx = null;

            try(session){
                Query q = session.createQuery("SELECT i.saltI FROM Items i WHERE i.id = :idI AND i.idUi = :idU");
                q.setParameter("idI", item.getIdI());
                q.setParameter("idU", user.getIdU());

                pc.setToken(itemsPepper);

                item.setSaltI((String) q.uniqueResult());
                item.setModification(new Timestamp(System.currentTimeMillis()));
                item.encrypt(pc);

                tx = session.beginTransaction();

                session.update(item);
                tx.commit();
            }catch (Exception e){
                if(tx != null) tx.rollback();
                return 202;
            }

            return 200;
        }else{
            return 201;
        }

    }

    public int deleteItem(Items item, Users user){
        if(Objects.equals(item.getIdUi(), user.getIdU())){

            Session session = sf.openSession();
            Transaction tx = session.beginTransaction();
            item.setSaltI("");

            try(session){
                session.delete(item);
                tx.commit();
                return 200;
            }catch (HibernateException e){
                tx.rollback();
                return 202;
            }

        }else{
            return 201;
        }
    }

    public void addDevice(Devices device, Users user){
        Session session = sf.openSession();
        Transaction tx = null;

        try(session){
            pc.setToken(devicesPepper);
            String hashIp = pc.hashData(device.getIp());
            String hashMac = pc.hashData(device.getMac());

            Query query = session.createQuery("From Devices d where d.usersByIdUd.id = :idU and d.ip = :ip and d.mac = :mac");
            query.setParameter("idU", user.getIdU());
            query.setParameter("ip", hashIp);
            query.setParameter("mac", hashMac);

            Devices queryDevice = (Devices) query.uniqueResult();

            tx = session.beginTransaction();

            System.out.println(queryDevice == null);

            if(queryDevice != null)
            {
                queryDevice.setLastLogin(new Date());
                queryDevice.setStateD((byte) 1);

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
        }
    }

    public int clearDevice(Users user){
        Session session = sf.openSession();
        Transaction tx = session.beginTransaction();

        try(session){
            Query q = session.createQuery("UPDATE Devices d set d.stateD = 0 where d.usersByIdUd.idU = :user");
            q.setParameter("user", user.getIdU());
            q.executeUpdate();

            tx.commit();

            return 200;
        }catch (HibernateException e){
            tx.rollback();
            return 202;
        }
    }

    public int updateUser(Users user){
        Session session = sf.openSession();
        Transaction tx = session.beginTransaction();

        try(session){
            session.saveOrUpdate(user);
            tx.commit();

            return 200;

        }catch (HibernateException e){
            tx.rollback();
            return 202;
        }
    }

    public int deleteUserAccount(Users user){
        Session session = sf.openSession();
        Transaction tx = session.beginTransaction();

        try(session){
            Query q = session.createQuery("DELETE Items i WHERE i.idUi = :id");
            q.setParameter("id", user.getIdU());

            q.executeUpdate();
            session.saveOrUpdate(user);
            tx.commit();

            return 200;

        }catch (HibernateException e){
            tx.rollback();
            return 202;
        }
    }
}
