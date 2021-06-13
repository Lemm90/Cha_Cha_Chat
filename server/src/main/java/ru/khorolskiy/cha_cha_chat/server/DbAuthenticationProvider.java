package ru.khorolskiy.cha_cha_chat.server;

import java.sql.*;

public class DbAuthenticationProvider implements AuthenticationProvider{
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


