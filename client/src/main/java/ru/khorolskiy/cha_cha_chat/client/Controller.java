package ru.khorolskiy.cha_cha_chat.client;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.URL;
import java.util.ResourceBundle;

public class Controller implements Initializable {

    @FXML
    TextField msgField, loginField, newUsernameField, newPasswordField, newNicknameField;

    @FXML
    PasswordField passwordField;

    @FXML
    TextArea msgArea;

    @FXML
    HBox loginPanel, msgPanel, regPanel;

    @FXML
    ListView<String> clientsList;

    public static final Logger LOGGER = LogManager.getLogger(Controller.class);
    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;
    private String username;
    private boolean isRegistration = false;
    private static final String EXIT = "/exit";

    public void setUsername(String username)  {
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
    }

    public void connect() {
        try {
            socket = new Socket("localhost", 8189);
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
                        }
                        if (msg.startsWith("/login_failed ")) {
                            String cause = msg.split("\\s", 2)[1];
                            msgArea.appendText(cause + "\n");
                        }
                    }
                    // Цикл общения
                    while (true) {
                        String msg = in.readUTF();
                        if(msg.startsWith("/")) {
                            if (msg.equals(EXIT)) {
                                msgArea.clear();
                                loginField.clear();
                                socket.close();
                                continue;
                            }
                            if(msg.startsWith("/clients_list ")){
                                String[] tokens = msg.split("\\s");

                                Platform.runLater(()-> {
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

    public void sendHelp(ActionEvent actionEvent) {
        msgArea.appendText(" /stat - количество сообщений \n " +
                "/who_am_i - Ваше имя \n " +
                "/w 'username' - личное сообщение \n " +
                "/exit - выход \n" +
                "/change_nick 'your new username' - сменить никнейм\n");
    }

    public void logout(ActionEvent actionEvent) throws IOException {
        msgArea.clear();
        loginField.clear();
        passwordField.clear();
        socket.close();
    }

    private void showErrorAlert(String message){
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setContentText(message);
        alert.setTitle("Cha-cha-Chat");
        alert.setHeaderText(null);
        alert.showAndWait();
    }

    private void showCompletedAlert(String message){
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
    }

    public void create(ActionEvent actionEvent) {

        if(newUsernameField.getText().isEmpty() || newPasswordField.getText().isEmpty() || newNicknameField.getText().isEmpty()){
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
        newNicknameField.clear();
        newPasswordField.clear();
        newUsernameField.clear();
        isRegistration = false;
        setUsername(null);
        showCompletedAlert("Регистрация прошла успешно");


    }
}
