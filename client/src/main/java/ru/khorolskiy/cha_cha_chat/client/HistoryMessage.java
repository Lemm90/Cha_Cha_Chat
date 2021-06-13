package ru.khorolskiy.cha_cha_chat.client;

import java.io.*;
import java.nio.charset.StandardCharsets;

public class HistoryMessage {
    private static File history;
    private String login;
    private static OutputStream outHistory;

    public void connect(String login) {
        history = new File(getFileName());
        this.login = login;
        try {
            outHistory = new FileOutputStream(getFileName(), true);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }
    //todo надо впилить дисконект
    public void disconnect() {
        login = null;
        if(outHistory != null){
            try{
                outHistory.close();
            } catch (IOException e){
                e.printStackTrace();
            }
        }
    }

    public void writingMessagesToHistory(String message) {
        String str = message + "\n";
        try {
            outHistory.write(str.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String outputHistory() throws IOException {
        StringBuilder stringBuilder = new StringBuilder();
        try (BufferedReader inHistory = new BufferedReader(new FileReader(getFileName()))) {
            String str;
            while ((str = inHistory.readLine()) != null) {
               stringBuilder.append(str).append("\n");
            }
            return stringBuilder.toString();
        }
    }

    private String getFileName(){
        String fileName = "History."+login+".txt";
        return fileName;
    }
}
