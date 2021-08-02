package ru.khorolskiy.cha_cha_chat.server;

import javafx.scene.control.Alert;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.*;

public class DbAuthenticationProvider implements AuthenticationProvider{
    public static final Logger LOGGER = LogManager.getLogger(DbAuthenticationProvider.class);
    private static Connection connection;
    private static Statement stmt;


    @Override
    public String getNicknameByLoginAndPssword(String login, String password) throws SQLException {
        String query = String.format("select nickname from clients where login = '%s' and password = '%s';", login, password);
        try ( ResultSet rs = stmt.executeQuery(query)){
            while (rs.next())
                return rs.getString("nickname");
        }
        return null;
    }

    @Override
    public void changeNickname(String oldNickname, String newNickname) {
        String query = String.format("update clients set nickname = '%s' where nickname = '%s';", newNickname, oldNickname);
        try {
            stmt.executeUpdate(query);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean userVerification(String nickname) throws SQLException {
        String query = String.format("select nickname from clients where nickname = '%s';", nickname);
        try (ResultSet rs = stmt.executeQuery(query)){
            if (rs.next()){
                return true;
            }
        }
        return false;
    }

    @Override
    public void creatingNewUser(String newUsername, String newPassword, String newNickname) throws SQLException {
        String query = String.format("insert into clients (login, password, nickname) values ('%s', '%s', '%s');", newUsername, newPassword, newNickname);
        try {
            stmt.executeUpdate(query);
            } catch (SQLException e) {
              e.printStackTrace();
              }
    }

    public boolean checkingNewUsername(String newUsername) {
        String query = String.format("select nickname from clients where login = '%s';", newUsername);
        try (ResultSet rs = stmt.executeQuery(query)){
            LOGGER.debug("Отправка запроса к БД: " + query);
            if (rs.next()){
                LOGGER.debug("/createUser_failed / Данное имя " + newUsername + " не уникально в БД");
                return true;
            }
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
        LOGGER.debug("/createUser_ok (checkingNewUsername)");
        return false;
    }

    public boolean checkingNewNickname(String newNickname) {
        String query = String.format("select nickname from clients where nickname = '%s';", newNickname);
        try (ResultSet rs = stmt.executeQuery(query)){
            LOGGER.debug("Отправка запроса к БД: " + query);
            if (rs.next()){
                LOGGER.debug("/createUser_failed / Данный никнейм " + newNickname + " не уникален в БД");
                return true;
            }
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
        LOGGER.debug("/createUser_ok (checkingNewNickname)");
        return false;
    }

        public static void connect() {
        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:clients.db");
            stmt = connection.createStatement();
        } catch (ClassNotFoundException | SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("Невозможно подключиться к БД");
        }
    }

    public static void disconnect(){
        try {
            if(stmt != null){
                stmt.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        if(connection != null){
            try {
                connection.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
}


