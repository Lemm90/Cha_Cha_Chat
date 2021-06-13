package ru.khorolskiy.cha_cha_chat.server;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.net.Socket;
import java.sql.SQLException;

public class ClientHandler {
    //todo logger откорректировать во всем чате
    public static final Logger LOGGER = LogManager.getLogger(ClientHandler.class);
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
                        // login Bob 100500

                        String[] tokens = msg.split("\\s+");
                        if(tokens.length !=3){
                            sendMessage("/login_failed Введите имя пользователя и пароль");
                            continue;
                        }

                        String login = tokens[1];
                        String password = tokens[2];

                        String userNickname = server.getAuthenticationProvider().getNicknameByLoginAndPssword(login, password);
                        if(userNickname == null){
                            sendMessage("/login_failed Введен некорректный логин/пароль");
                            continue;
                        }
                        if (server.isUserOline(userNickname)) {
                            sendMessage("/login_failed Учетная запись уже используется");
                            continue;
                        }
                        username = userNickname;
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
            } catch (IOException | SQLException e) {
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
        if (command.equals(STATISTIC)) {
            try {
                out.writeUTF("Количество ваших сообщений за сессию составляет: " + numberOfMessage);
            } catch (IOException e) {
                disconnect();
            }
            LOGGER.info(String.format("Клиент '%s' ввел комманду '%s'", getUsername(), STATISTIC) );
            return;
        }

        if (command.equals(EXIT)) {
            LOGGER.info(String.format("Клиент '%s' ввел комманду '%s'", getUsername(), EXIT) );
            try {
                out.writeUTF(command);
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }

        if (command.equals(WHATISMYNAME)) {
            try {
                out.writeUTF("Ваш ник: " + username);
            } catch (IOException e) {
                disconnect();
            }
            LOGGER.info(String.format("Клиент '%s' ввел комманду '%s'", getUsername(), WHATISMYNAME) );
            return;
        }

        if (command.startsWith("/w")) {
            String[] privatString = command.split("\\s", 3);
            if (privatString.length != 3) {
                sendMessage("Введена некорректная команда");
                return;
            }
            server.privateMessage(this, privatString[1], privatString[2]);
            LOGGER.info(String.format("Клиент %s ввел комманду приватных сообщений", getUsername()) );
            return;
        }
        if (command.startsWith(CHANGE_NICK)) {
            String[] tokens = command.split("\\s+");
            if (tokens.length != 2) {
                sendMessage("Введена некорректная команда");
                LOGGER.info(String.format("Клиент '%s' ввел неккорректную команду: '%s'", getUsername(), command) );
                return;
            }
            String newNick = tokens[1];
            try {
                if (server.getAuthenticationProvider().userVerification(newNick)) {
                    sendMessage("К сожалению такой никнейм уже существует. Придумайте новый ник.");
                    LOGGER.info(String.format("Клиент '%s' ввел команду: '%s' с существующим никнеймом '%s'", getUsername(), CHANGE_NICK, tokens[1]) );
                    return;
                }
            } catch (SQLException throwables) {
                throwables.printStackTrace();
            }
            LOGGER.info(String.format("Клиент '%s' сменил свой ник на: '%s'", getUsername(), tokens[1]) );
            server.getAuthenticationProvider().changeNickname(this.username, newNick);
            username = newNick;
            sendMessage("Вы изменили никнейм на: " + newNick);
            server.broadcastClientsList();
        }
    }

    public void disconnect() {
        server.unsubscribe(this);
        if (socket != null) {
            try {
                LOGGER.info(String.format("Клиент %s закрыл соединение", getUsername()));
                socket.close();
            } catch (IOException e) {
                LOGGER.error("При socket.close() оказалось socket == null");
                e.printStackTrace();
            }
        }
    }
}
