package com.keyvault.controllers;

import com.keyvault.Request;
import com.keyvault.Response;
import com.keyvault.SecureSocket;
import com.keyvault.database.models.*;
import javax.crypto.*;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.Socket;
import java.security.InvalidKeyException;

public class ClientRequestController extends Thread{
    private String userP, itemP, deviceP, redisPassword;
    private AuthController authController;
    private SecureSocket secureSocket;

    public ClientRequestController(Socket clientSocket, String[] p) throws IOException {
        secureSocket = new SecureSocket(clientSocket);
        userP = p[0];
        itemP = p[1];
        deviceP = p[2];
        redisPassword = p[3];
    }

    @Override
    public void run(){
        try {
            Request request = (Request) secureSocket.readObject();
            authController = new AuthController(userP, deviceP, redisPassword);

            if(request != null)
            {
                if(request.getToken() == null){
                    if(request.getUser() != null && request.getDevice() != null)
                        requestHandler(request, new Controller(itemP, deviceP));
                    else
                        sendResponse(203, null);
                }else{
                    requestPrivilegeHandler(request, new Controller(itemP, deviceP));
                }
            }

            secureSocket.close();

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
        Users user = request.getUser();
        Devices device = request.getDevice();

        if(user != null && device != null){
            device.setIp(secureSocket.getHost());

            int error;

            switch (request.getOperationCode()) {
                case "LOGIN" -> {
                    //Check if the client can be authenticated by user and device
                    error = authController.authenticate(user, device);

                    if (error == 102 && !authController.getAuthUser().isTotpverified())
                        authController.generateVerifyToken();

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
                        String[] code = secureSocket.readUTF().trim().split("::");
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
     */
    private void requestPrivilegeHandler(Request request, Controller controller) {
        SessionToken token = request.getToken();
        Object requestObject = request.getContent();

        if(checkToken(token)){
            Users user = authController.getAuthUser();

            switch (request.getOperationCode()){
                case "TOTP" -> {
                    int operationStatus = controller.updateUser(authController.controlTOTP());
                    boolean needQR = !user.isTotpverified() && user.getKey2Fa() != null;

                    sendResponse(operationStatus, needQR ? authController.getQR() : null);
                }// Manage TOTP
                case "GET" -> {
                    sendResponse(200, controller.getUserItems(user));
                } // Get items
                case "GET-DEVICES" -> {
                    sendResponse(200, controller.getUsersDevices(user));
                }
                case "INSERT" -> {
                    int id = controller.createItem((Items) requestObject);
                    sendResponse(id != -1 ? 200 : 202, id);
                } //Create item
                case "MOD" -> {
                    sendResponse(controller.modifyItem((Items) requestObject, user), null);
                } // Modify item
                case "DELETE" -> {
                    sendResponse(controller.deleteItem((Items) requestObject, user), null);
                } // Delete item
                case "CLEAR-DEVICE" -> {
                    sendResponse(controller.clearDevice(user), null);
                } // Remove all user devices
                case "DELETE-USER" -> {
                    user.setKey2Fa(null);
                    user.setStateU(false);

                    sendResponse(controller.deleteUserAccount(user), null);
                }
                case "VERIFY-TOTP" -> {
                    sendResponse(authController.verify2FA((String) requestObject), null);
                }
                case "PROFILE-IMAGE" -> {
                    try {
                        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream((byte[]) requestObject);
                        BufferedImage image = ImageIO.read(byteArrayInputStream);

                        sendResponse(controller.saveImage(image, user ), null);
                    } catch (IOException e) {
                        sendResponse(202, null);
                    }
                }
                case "GET-PROFILE-IMAGE" -> {
                    ByteArrayOutputStream image = controller.getImage(user);

                    sendResponse(image == null ? 202 : 200, image);
                }
                default -> sendResponse(203, null);
            }

            authController.revalidateToken();
        }

    }

    private boolean checkToken(SessionToken token){
        boolean auth = authController.checkSessionToken(token);
        if(!auth) sendResponse(201, null);

        return auth;
    }

    private void sendResponse(int code, Object objects){
        try {
            Response response = new Response(code, objects);
            secureSocket.writeObject(response);

        } catch (BadPaddingException | IOException | IllegalBlockSizeException |
                 InvalidKeyException e) {
            e.printStackTrace();
        }
    }
}