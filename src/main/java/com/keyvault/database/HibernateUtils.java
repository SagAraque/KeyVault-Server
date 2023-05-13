package com.keyvault.database;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.service.ServiceRegistry;

public class HibernateUtils {
    private static final SessionFactory sessionFactory = buildSessionFactory();
    private static final ThreadLocal<Session> sessionThread = new ThreadLocal<>();

    private static SessionFactory buildSessionFactory(){
        ServiceRegistry service = new StandardServiceRegistryBuilder()
                .configure()
                .build();

        return new MetadataSources(service).buildMetadata().buildSessionFactory();
    }

    public static SessionFactory getSessionFactory(){
        return sessionFactory;
    }

    public static Session getCurrentSession()
    {
        Session session = sessionThread.get();

        if(session == null || !session.isOpen())
        {
            session = sessionFactory.openSession();
            sessionThread.set(session);
        }

        return session;
    }

    public static void closeSession(Session session)
    {
        if(session != null)
        {
            session.close();
            sessionThread.remove();
        }
    }
}
