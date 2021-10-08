package ru.khorolskiy.cha_cha_chat.client;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.URL;
import java.util.ResourceBundle;

public class Controller implements Initializable {

    @FXML
    TextField msgField, loginField, newUsernameField, newPasswordField, newNicknameField, changeNickField;

    @FXML
    PasswordField passwordField;

    @FXML
    TextArea msgArea;

    @FXML
    HBox loginPanel, msgPanel, regPanel, changeNickPanel;

    @FXML
    ListView<String> clientsList;

    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;
    private String username;
    private boolean isRegistration;
    private boolean createUsername = false;
    private boolean createNickname = false;
    private static final String EXIT = "/exit";


    public void setUsername(String username) {
        this.username = username;
        boolean usernameIsNull = username == null;
        loginPanel.setVisible(usernameIsNull);
        loginPanel.setManaged(usernameIsNull);
        msgPanel.setVisible(!usernameIsNull);
        msgPanel.setManaged(!usernameIsNull);
        clientsList.setVisible(!usernameIsNull);
        clientsList.setManaged(!usernameIsNull);
        regPanel.setVisible(isRegistration);
        regPanel.setManaged(isRegistration);
        changeNickPanel.setVisible(false);
        changeNickPanel.setManaged(false);
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setUsername(null);
    }

    public void login() {

        if (loginField.getText().isEmpty()) {
            showErrorAlert("Имя пользователя не может быть пустым");
            return;
        }

        if (socket == null || socket.isClosed()) {
            connect();
        }

        try {
            out.writeUTF("/login " + loginField.getText() + " " + passwordField.getText());
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        clearingLoginPanel();
    }

    public void connect() {
        try {
            socket = new Socket("localhost", 8789);
            in = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());
            HistoryMessage historyMessage = new HistoryMessage();
            Thread t = new Thread(() -> {
                try {
                    // Цикл авторизации
                    while (true) {
                        String msg = in.readUTF();
                        if (msg.startsWith("/login_ok ")) {
                            setUsername(msg.split("\\s")[1]);
                            msgArea.clear();
                            historyMessage.connect(loginField.getText());
                            msgArea.clear();
                            msgArea.appendText(historyMessage.outputHistory());
                            break;
                        } else if (msg.startsWith("/createUser_ok")) {
                            createUsername = true;
                            createNickname = true;
                            break;
                        } else if (msg.startsWith("/createUser_failed")) {
                            createUsername = false;
                            break;
                        } else if (msg.startsWith("/createNickname_failed")) {
                            createUsername = true;
                            createNickname = false;
                            break;
                        } else if(msg.startsWith("/login_failed")){
                            String[] tokens = msg.split(" ", 2);
                            System.out.println(tokens[1]);
                            showErrorAlert(" ");
                        }
                    }

                    // Цикл общения
                    while (true) {
                        String msg = in.readUTF();
                        if (msg.startsWith("/")) {
                            if (msg.equals(EXIT)) {
                                msgArea.clear();
                                loginField.clear();
                                socket.close();
                                continue;
                            }
                            if (msg.startsWith("/clients_list ")) {
                                String[] tokens = msg.split("\\s");

                                Platform.runLater(() -> {
                                    clientsList.getItems().clear();
                                    for (int i = 1; i < tokens.length; i++) {
                                        clientsList.getItems().add(tokens[i]);
                                    }
                                });
                                continue;
                            }
                        }
                        msgArea.appendText(msg + "\n");
                        historyMessage.writingMessagesToHistory(msg);
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    disconnect();
                }
            });
            t.start();
        } catch (IOException e) {
            showErrorAlert("Невозможно подключиться к серверу");
        }
    }

    public void sendMsg() {
        try {
            out.writeUTF(msgField.getText());
            msgField.clear();
            msgField.requestFocus();
        } catch (IOException e) {
            showErrorAlert("Невозможно отправить сообщение");
        }
    }

    public void disconnect() {
        setUsername(null);
        try {
            if (socket != null) {
                socket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void buttonHelp(ActionEvent actionEvent) {
        msgArea.appendText(" /stat - количество сообщений \n " +
                "/who_am_i - Ваше имя \n " +
                "/w 'username' - личное сообщение \n " +
                "/exit - выход \n" +
                "/change_nick 'your new username' - сменить никнейм\n");
    }

    public void buttonLogout(ActionEvent actionEvent) throws IOException {
        msgArea.clear();
        socket.close();
    }

    public void buttonChangeNick(ActionEvent actionEvent) {
        loginPanel.setVisible(false);
        loginPanel.setManaged(false);
        msgPanel.setVisible(false);
        msgPanel.setManaged(false);
        clientsList.setVisible(false);
        clientsList.setManaged(false);
        regPanel.setVisible(false);
        regPanel.setManaged(false);
        changeNickPanel.setManaged(true);
        changeNickPanel.setVisible(true);
        msgArea.setVisible(false);
        msgArea.setManaged(false);
    }

    public void sendChangeNick(ActionEvent actionEvent) {
    }


    private void showErrorAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setContentText(message);
        alert.setTitle("Cha-cha-Chat");
        alert.setHeaderText(null);
        alert.showAndWait();
    }

    private void showCompletedAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setContentText(message);
        alert.setTitle("Cha-cha-Chat");
        alert.setHeaderText(null);
        alert.showAndWait();
    }

    //todo надо что-то с этим придумать, что б красиво было
    public void register(ActionEvent actionEvent) {
        loginPanel.setVisible(false);
        loginPanel.setManaged(false);
        msgPanel.setVisible(false);
        msgPanel.setManaged(false);
        clientsList.setVisible(false);
        clientsList.setManaged(false);
        regPanel.setVisible(true);
        regPanel.setManaged(true);
        changeNickPanel.setManaged(false);
        changeNickPanel.setVisible(false);
    }

    public void create(ActionEvent actionEvent) {

        if (newUsernameField.getText().isEmpty() || newPasswordField.getText().isEmpty() || newNicknameField.getText().isEmpty()) {
            showErrorAlert("Все поля должны быть заполнены");
        }
        if (socket == null || socket.isClosed()) {
            connect();
        }
        String newUser = String.format("/createUser %s %s %s", newUsernameField.getText(), newPasswordField.getText(), newNicknameField.getText());
        try {
            out.writeUTF(newUser);
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        registrationAnnouncement();
    }

    public void clearingLoginPanel() {
        loginField.clear();
        passwordField.clear();
    }

    public void clearingRegPanel() {
        newNicknameField.clear();
        newPasswordField.clear();
        newUsernameField.clear();
    }


    public void registrationAnnouncement() {
        if (createNickname && createUsername) {
            showCompletedAlert("Регистрация прошла успешно");
            setUsername(null);
            clearingRegPanel();
        } else if (!createUsername) {
            showErrorAlert("К сожалению такое имя уже существует. Придумайте новое имя.");
            clearingRegPanel();
        } else if (!createNickname) {
            showErrorAlert("К сожалению такой никнейм уже существует. Придумайте новый ник.");
            clearingRegPanel();
        }
    }


}
