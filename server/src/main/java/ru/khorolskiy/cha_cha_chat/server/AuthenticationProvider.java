package ru.khorolskiy.cha_cha_chat.server;

public interface AuthenticationProvider {

    String getNicknameByLiginAndPssword(String login, String password);
    void changeNickname(String oldNickname, String newNickname);
}
