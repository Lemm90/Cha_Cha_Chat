package ru.khorolskiy.cha_cha_chat.server;

import java.sql.SQLException;

public interface AuthenticationProvider {

    String getNicknameByLoginAndPssword(String login, String password) throws SQLException;
    void changeNickname(String oldNickname, String newNickname);
    boolean userVerification(String nickname) throws SQLException;
}
