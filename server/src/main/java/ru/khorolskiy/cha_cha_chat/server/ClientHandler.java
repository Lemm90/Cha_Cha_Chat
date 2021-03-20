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

    public void sendMessage(String message) throws IOException {
        out.writeUTF(message);
    }

    public void commandMessage(String command) throws IOException {
        if(command.equals(STATISTIC)){
            out.writeUTF("Количество ваших сообщений: " + numberOfMessage);
            return;
        }

        if(command.equals(EXIT)){
            out.writeUTF(command);
            socket.close();

        }

        if(command.equals(WHATISMYNAME)){
            out.writeUTF("Ваш ник: " + username);
            return;
        }

        if(command.startsWith("/w")){
            String[] privatString = command.split("\\s", 3);
            server.privateMessage(this, privatString[1], privatString[2]);
            return;
        }

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
