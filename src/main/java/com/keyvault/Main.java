package com.keyvault;

import com.keyvault.controllers.ClientRequestController;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.Properties;

public class Main {
    private static String[] peppers;
    public static void main(String[] args) throws IOException, NoSuchAlgorithmException, KeyManagementException {
        /*
        SSLContext context = SSLContext.getInstance("TLS");
        context.init(null, null, null);

        SSLServerSocketFactory factory = context.getServerSocketFactory();
        SSLServerSocket server = (SSLServerSocket) factory.createServerSocket(5556);
        */

        try {
            getPeppers(args[0]);
            ServerSocket server = new ServerSocket(5556);
            ClientRequest c;

            while(true) {
                c = new ClientRequest(server.accept(), peppers);
                c.start();
            }

        }catch (Exception e){
            e.printStackTrace();
        }
    }

    private static void getPeppers(String fileSource) throws IOException{
        Properties prop = new Properties();
        prop.load(new FileInputStream(fileSource + "config.conf"));

        peppers = new String[]{prop.getProperty("users"), prop.getProperty("items"), prop.getProperty("devices")};
    }

}
