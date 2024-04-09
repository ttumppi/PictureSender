package com.example.PictureSender;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class ClientSocket {
    private Socket clientSocket;
    private PrintWriter out;
    private BufferedReader in;


    public void startConnection(String ip, int port) throws IOException {
        clientSocket = new Socket(ip, port);


    }

    public void sendMessage(byte[] data, String endMessage) throws IOException {

            String chars = "";
            byte[] answer = new byte[3];
            clientSocket.getOutputStream().write(data);
            clientSocket.getOutputStream().write(endMessage.getBytes());
            clientSocket.getOutputStream().flush();


            while (clientSocket.getInputStream().read(answer) != 0){
                chars += new String(answer, StandardCharsets.UTF_8);
                if (chars.equals(endMessage)){
                    break;
                }
                answer = new byte[3];


            }



    }



    public void stopConnection() throws IOException {


        clientSocket.close();
    }
}
