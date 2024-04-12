package com.example.PictureSender;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class ClientSocket {
    private Socket _clientSocket;




    public void startConnection(String ip, int port) throws IOException {
        _clientSocket = new Socket(ip, port);


    }

    public void sendMessage(byte[] data, String endMessage) throws IOException {

            String chars = "";
            byte[] answer = new byte[endMessage.length()];
            _clientSocket.getOutputStream().write(data);
            _clientSocket.getOutputStream().write(endMessage.getBytes());
            _clientSocket.getOutputStream().flush();

            while (_clientSocket.getInputStream().read(answer) != 0){
                chars += new String(answer, StandardCharsets.UTF_8);
                if (chars.equals(endMessage)){
                    break;
                }
                answer = new byte[endMessage.length()];


            }

    }

    public void Poll(){
        try{
            _clientSocket.getOutputStream().write(";;;".getBytes());
            _clientSocket.getOutputStream().flush();
        }
        catch(Exception e){

        }

    }

    public boolean TryPoll(String message, String ip, int port){
        try{
            startConnection(ip, port);
            byte[] answer = new byte[message.length()];
            _clientSocket.getOutputStream().write(message.getBytes(StandardCharsets.UTF_8));
            _clientSocket.getOutputStream().flush();
            Thread.sleep(5000L);




            _clientSocket.getInputStream().read(answer);

            return new String(answer, StandardCharsets.UTF_8).equals(message);
        }
        catch(Exception e){
            return false;
        }



    }

    public void stopConnection() throws IOException {


        _clientSocket.close();
    }
}
