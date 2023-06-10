package com.keyvault;

import com.keyvault.controllers.ClientRequestController;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Properties;

public class Main {
    private static String[] peppers;
    public static void main(String[] args){
        try {
            getPeppers(args[0]);
            ServerSocket server = new ServerSocket(5556);
            ClientRequestController clientRequestController;

            while(true) {
                Socket clientSocket = server.accept();
                clientSocket.setSoTimeout(10000);
                clientRequestController = new ClientRequestController(clientSocket, peppers);
                clientRequestController.start();
            }

        }catch (Exception e){
            e.printStackTrace();
        }
    }

    private static void getPeppers(String fileSource) throws IOException{
        Properties prop = new Properties();
        prop.load(new FileInputStream(fileSource + "config.conf"));

        peppers = new String[]{
                prop.getProperty("users"),
                prop.getProperty("items"),
                prop.getProperty("devices"),
                prop.getProperty("redis")
        };
    }

}
