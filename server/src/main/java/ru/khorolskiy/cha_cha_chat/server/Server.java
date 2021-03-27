package ru.khorolskiy.cha_cha_chat.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class Server {
    private int port;
    private List<ClientHandler> clients;
    private AuthenticationProvider authenticationProvider;

    public AuthenticationProvider getAuthenticationProvider() {
        return authenticationProvider;
    }

    public Server(int port) {
        this.port = port;
        this.clients = new ArrayList<>();
        this.authenticationProvider = new InMemoryAuthenticationProvider();
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Сервер запущен на порту " + port);
            while (true) {
                System.out.println("Ждем нового клиента..");
                Socket socket = serverSocket.accept();
                System.out.println("Клиент подключился");
                new ClientHandler(this, socket);
            }
        } catch (IOException e) {
            e.printStackTrace();
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
