package com.keyvault;

import com.keyvault.entities.Devices;
import com.keyvault.entities.Items;
import com.keyvault.entities.Tokens;
import com.keyvault.entities.Users;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class ClientRequest extends Thread{
    private final Socket client;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private String userP, itemP;
    private AuthController authController;

    public ClientRequest(Socket clientSocket, String[] p){
        client = clientSocket;
        userP = p[0];
        itemP = p[1];
    }

    @Override
    public void run(){
        try {
            authController = new AuthController(userP);
            in = new ObjectInputStream(client.getInputStream());
            out = new ObjectOutputStream(client.getOutputStream());

            Request request = (Request) in.readObject();

            if(request.getToken() == null){
                if(request.getContent() != null){
                    requestHandler(request, new Controller(itemP));
                }else{
                    sendResponse(203, null);
                }
            }else{
                requestPrivilegeHandler(request, new Controller(itemP));
            }


            in.close();
            out.close();
            client.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Handles unprivileged requests such as login or registration.
     * @param request Client request
     * @param controller
     * @throws Exception
     */
    private void requestHandler(Request request, Controller controller) throws Exception {
        Users user = (Users) request.getContent()[0];
        Devices device = (Devices) request.getContent()[1];

        if(user != null && device != null){
            device.setIp(client.getInetAddress().getHostAddress());

            int error;

            switch (request.getOperationCode()) {
                case "LOGIN" -> {
                    //Check if the client can be authenticated by user and device
                    error = authController.authenticate(user, device);

                    if(error == 102)
                        if(!authController.getAuthUser().isHas2fa())
                            authController.generateAuthCode();

                    sendResponse(error, error == 200 ? authController.generateToken() : null);
                }
                case "REGISTER" ->{
                    error = authController.createUser(user, device);
                    sendResponse(error, null);
                }
                case "VERIFY" ->{
                    error = authController.authenticate(user, device);

                    sendResponse(error, null);

                    if(error == 102){
                        String[] code = in.readUTF().trim().split("::");
                        error = authController.validate2FA(code[0]);

                        if(error == 200 && code[1].equals("1"))
                            controller.addDevice(device, authController.getAuthUser());
                    }

                    sendResponse(error, error == 200 ? authController.generateToken() : null);

                }
                default -> sendResponse(203, null);

            }
        }else{
            sendResponse(203, null);
        }

    }

    /**
     * Handles privileged requests, which will need a token provided by the client in the request.
     * @param request Client request
     * @param controller
     * @throws Exception
     */
    private void requestPrivilegeHandler(Request request, Controller controller) throws Exception {
        Tokens token = request.getToken();
        Object[] requestObjects = request.getContent();

        if(checkToken(token)){
            Users user = authController.getAuthUser();

            switch (request.getOperationCode()){
                case "TOTP" -> {
                    int operationStatus = controller.updateUser(authController.controlTOTP());
                    sendResponse(operationStatus, user.isHas2fa() ? authController.getQR() : null);
                }// Manage TOTP
                case "GET" -> {
                    sendResponse(200, controller.getUserItems(user));
                } // Get items
                case "INSERT" -> {
                    sendResponse(controller.createItem((Items) requestObjects[0], user), null);
                } //Create item
                case "MOD" -> {
                    sendResponse(controller.modifyItem((Items) requestObjects[0], user), null);
                } // Modify item
                case "DELETE" -> {
                    sendResponse(controller.deleteItem((Items) requestObjects[0], user), null);
                } // Delete item
                case "CLEAR-DEVICE" -> {
                    sendResponse(controller.clearDevice(user), null);
                } // Remove all user devices
                case "DELETE-USER" -> {
                    user.setKey2Fa(null);
                    user.setStateU((byte) 0);

                    sendResponse(controller.deleteUserAccount(user), null);
                }
                default -> sendResponse(203, null);
            }
        }

    }

    private boolean checkToken(Tokens token){
        boolean auth = authController.checkSessionToken(token);
        if(!auth) sendResponse(201, null);

        return auth;
    }

    private void sendResponse(int code, Object objects){
        try {
            authController.revalidateToken();
            out.writeObject(new Response(code, objects));
            out.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}