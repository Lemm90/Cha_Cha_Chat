package ru.khorolskiy.cha_cha_chat.server;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class Server {

    private int port;
    private List<ClientHandler> clients;
    private DbAuthenticationProvider authenticationProvider;
    public AuthenticationProvider getAuthenticationProvider() {
        return authenticationProvider;
    }
    public static final Logger LOGGER = LogManager.getLogger(Server.class);
    public Server(int port){
        this.port = port;
        this.clients = new ArrayList<>();
        this.authenticationProvider = new DbAuthenticationProvider();
        this.authenticationProvider.connect();
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            LOGGER.info(String.format("Серевер запущен на порту: %s\n", port));
            while (true) {
                System.out.println("Ждем нового клиента..");
                Socket socket = serverSocket.accept();
                new ClientHandler(this, socket);
                LOGGER.info("Клиент подключился");

            }
        } catch (IOException e) {
            LOGGER.error("Клиенту не удалось подключиться");
            e.printStackTrace();
        } finally {
            this.authenticationProvider.disconnect();
        }
    }

    public synchronized void subscribe(ClientHandler clientHandler){
        clients.add(clientHandler);
        broadcastMessage("Клиент " +clientHandler.getUsername() + " вошел в чат" );
        broadcastClientsList();

    }

    public synchronized void unsubscribe(ClientHandler clientHandler) {
        clients.remove(clientHandler);
        broadcastMessage("Клиент " +clientHandler.getUsername() + " вышел из чата" );
        broadcastClientsList();

    }

    public synchronized void broadcastMessage(String message){
        for (ClientHandler clientHandler : clients) {
            clientHandler.sendMessage(message);
        }
    }

    public synchronized void privateMessage(ClientHandler sender, String username, String message) {
        for (ClientHandler c : clients) {
            if(c.getUsername().equals(username)){
                c.sendMessage("От: " + sender.getUsername() + " Сообщение: " + message);
                sender.sendMessage("Пльзователю: " + username+ " Сообщение: " + message);
                return;
            }
        }
        sender.sendMessage("Пользователя " + username + "нет в сети");
    }


    public synchronized boolean isUserOline(String username) {
        for (ClientHandler clientHandler : clients) {
            if (clientHandler.getUsername().equals(username)) {
                return true;
            }
        }
        return false;
    }

    public synchronized void broadcastClientsList(){
        StringBuilder stringBuilder = new StringBuilder("/clients_list ");
        for (ClientHandler c: clients) {
            stringBuilder.append(c.getUsername()).append(" ");
        }
            stringBuilder.setLength(stringBuilder.length() - 1);
            String clientsList = stringBuilder.toString();
            for (ClientHandler clientHandler : clients) {
            clientHandler.sendMessage(clientsList);
        }
    }

}
