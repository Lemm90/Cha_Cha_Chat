package ru.khorolskiy.cha_cha_chat.client;

import java.io.*;

public class HistoryMessage {
    private static File history;
    private static OutputStream outHistory;
    private static InputStream inHistory;

    public void connect(){
        history = new File("history.txt");
        try {
            outHistory = new FileOutputStream("history.txt", true);
            // todo надо что-то сделать с форматом вывода
            inHistory = new BufferedInputStream( new FileInputStream("history.txt"));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    public static void disconnect(){
        try {
            inHistory.close();
            outHistory.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void writingMessagesToHistory(String message){
        String s = message + "\n";
        byte[] msgHistory = s.getBytes();
        try {
            outHistory.write(msgHistory);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void outputHistory(Controller controller) throws IOException {
        StringBuilder stringBuilder = new StringBuilder();
        int x;
        while (( x = inHistory.read()) != -1){
            stringBuilder.append((char)x);
        }
        controller.msgArea.appendText(stringBuilder.toString());

    }
}
