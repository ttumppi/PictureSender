package com.example.PictureSender;

import android.net.wifi.p2p.WifiP2pManager;
import android.util.Xml;

import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class ListenerSocket {

    private ServerSocket _socket;

    OnImageCommandReceived _onCommand;

    private boolean _running;

    private Thread _thread;

    private final String _endMessage = "ACK";

    private final byte[] _endMessageInBytes = _endMessage.getBytes(StandardCharsets.UTF_8);

    public ListenerSocket(OnImageCommandReceived callBack)
    {
        _onCommand = callBack;
        _running = false;
        _thread = CreateThread();
        try{
            _socket = new ServerSocket(33666);

        }
        catch(Exception e){}
    }

    public void Start(){
        _thread.start();
    }

    private Thread CreateThread(){
        return new Thread(() -> {
            Socket comm = null;
           while (_running){

               if (comm == null){

                   try{
                       comm = _socket.accept();
                   }
                   catch(Exception e){}
               }

               if (!CheckConnection(comm)){
                   comm = null;
                   continue;
               }

               if (DataAvailable(comm)){
                   _onCommand.OnCommandReceived();
                   ClearInputStream(comm);
                   SendAcknowledge(comm);
               }

           }
        });
    }

    private boolean DataAvailable(Socket socket){
        try {
            return socket.getInputStream().available() > 0;
        }
        catch(Exception e){}
        return false;
    }

    private void ClearInputStream(Socket socket){
        try{
            while (socket.getInputStream().read() != -1){

            }
        }
        catch (Exception e){}
    }

    private void SendAcknowledge(Socket socket){
        try{
            socket.getOutputStream().write(_endMessageInBytes);
            socket.getOutputStream().flush();
        }
        catch(Exception e){}
    }

    private boolean CheckConnection(Socket socket){
        try{
            socket.getOutputStream().write(new byte[0]);
        }
        catch(Exception e){
            return false;
        }
        return true;
    }

    public void Shutdown(){
        _running = false;
        try{
            _socket.close();
        }
        catch(Exception e){}
    }
}
