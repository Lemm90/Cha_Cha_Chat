package ru.khorolskiy.cha_cha_chat.server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class ClientHandler {
    private Server server;
    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;
    private String username;
    private int numberOfMessage;
    private static final String WHATISMYNAME = "/who_am_i";
    private static final String STATISTIC = "/stat";
    private static final String EXIT = "/exit";
    private static final String CHANGE_NICK = "/change_nick";

    public String getUsername() {
        return username;
    }

    public ClientHandler(Server server, Socket socket) throws IOException {
        this.server = server;
        this.socket = socket;
        this.in = new DataInputStream(socket.getInputStream());
        this.out = new DataOutputStream(socket.getOutputStream());
        new Thread(() -> {
            try {
                // Цикл авторизации
                while (true) {
                    String msg = in.readUTF();
                    if (msg.startsWith("/login ")) {
                        // login Bob
                        String usernameFromLogin = msg.split("\\s")[1];

                        if (server.isUserOline(usernameFromLogin)) {
                            sendMessage("/login_failed Current nickname is already used");
                            continue;
                        }

                        username = usernameFromLogin;
                        sendMessage("/login_ok " + username);
                        server.subscribe(this);
                        break;
                    }
                }
                // Цикл общения с клиентом
                while (true) {
                    String msg = in.readUTF();
                    if(msg.startsWith("/")){
                        commandMessage(msg);
                        continue;
                    }
                    server.broadcastMessage(username + ": " + msg);
                    numberOfMessage++;
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                disconnect();
            }
        }).start();
    }

    public void sendMessage(String message){
        try {
            out.writeUTF(message);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void commandMessage(String command) {
        if(command.equals(STATISTIC)){
            try {
                out.writeUTF("Количество ваших сообщений: " + numberOfMessage);
            } catch (IOException e) {
                disconnect();
            }
            return;
        }

        if(command.equals(EXIT)){
            try {
                out.writeUTF(command);
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }

        if(command.equals(WHATISMYNAME)){
            try {
                out.writeUTF("Ваш ник: " + username);
            } catch (IOException e) {
                disconnect();
            }
            return;
        }

        if(command.startsWith("/w")){
            String[] privatString = command.split("\\s", 3);
            server.privateMessage(this, privatString[1], privatString[2]);
            return;
        }

//      НЕ ДОДЕЛАЛ! --->
        
//        if(command.equals(CHANGE_NICK)){
//            String newNick = command.split("\\s")[1];
//            if(!server.isUserOline(newNick)){
//                try {
//                    out.writeUTF("К сожалению такой ник уже существует. Придумайте новый ник.");
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//            }
//            username = newNick;
//            try {
//                out.writeUTF("Вы сменили ник на: " + username);
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//            return;
//        }
    }

    public void disconnect() {
        server.unsubscribe(this);
        if (socket != null) {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
