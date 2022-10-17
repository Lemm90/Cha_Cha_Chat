package ru.khorolskiy.cha_cha_chat.client;

import javafx.application.Platform;
import lombok.Data;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

@Data
public class Network {
    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;

    private CallBack onMessageReceivedCallBack;
    private CallBack onConnectCallBack;
    private CallBack onDisconnectCallBack;
    private CallBack onAuthOkCallBack;
    private CallBack onAuthFailedCallBack;
    private CallBack onCreateUserOk;
    private CallBack onCreateUserFailed;
    private CallBack onCreateNicknameFailed;


    public boolean isConnected(){
        return socket != null && !socket.isConnected();
    }

    public void connect(int port) throws IOException{
            socket = new Socket("localhost", port);
            in = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());
           if(onConnectCallBack != null) {
               onConnectCallBack.callBack();
           }
            Thread t = new Thread(() -> {
                try {
                    while (true) {
                        String msg = in.readUTF();
                        if (msg.startsWith("/login_ok ")) {
                            if (onAuthOkCallBack != null) {
                                onAuthOkCallBack.callBack(msg);
                            }
                            break;
                        } else if (msg.startsWith("/createUser_ok")) {
                            if(onCreateUserOk != null){
                                onCreateUserOk.callBack(msg);
                            }
                        } else if (msg.startsWith("/createUser_failed")) {
                            if(onCreateUserFailed != null){
                                onCreateUserFailed.callBack(msg);
                            }
                        } else if (msg.startsWith("/createNickname_failed")) {
                            if(onCreateNicknameFailed != null){
                                onCreateNicknameFailed.callBack(msg);
                            }
                        } else if(msg.startsWith("/login_failed")){
                            String[] cause = msg.split(" ", 2);
                            if (onAuthFailedCallBack != null) {
                                onAuthFailedCallBack.callBack(cause);
                            }
                        }
                    }

                    while (true) {
                        String msg = in.readUTF();
                        if(onMessageReceivedCallBack != null) {
                            onMessageReceivedCallBack.callBack(msg);
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    disconnect();
                }
            });
            t.start();
    }

    public void sendMessage(String message) throws IOException {
        out.writeUTF(message);
    }

    public void tryToLogin(String login, String password) throws IOException {
        sendMessage("/login " + login + " " + password);
    }

    public void disconnect(){
        if(onDisconnectCallBack != null) {
            onDisconnectCallBack.callBack();
        }
        try {
            if(in != null){
                in.close();
            }
            if(out != null){
                out.close();
            }
            if (socket != null) {
                socket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
