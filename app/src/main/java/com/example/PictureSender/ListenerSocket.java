package com.example.PictureSender;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

public class ListenerSocket {
    private int _port;
    private GetIpCallBack _onReady;
    private DatagramSocket _serverSocket;
    private String _endOfMessage;

    private List<String> _blackList;
    public ListenerSocket(int port, String endOfMessage){
        _port = port;
        _endOfMessage = endOfMessage;
        _blackList = new ArrayList<>();
        try{
            _serverSocket = new DatagramSocket(port);
            _serverSocket.setBroadcast(true);
        }
        catch(Exception e){
            ServerSelectionActivity.ForDebugging = e.getMessage();
        }
    }

    public void StartListening(GetIpCallBack onReady){
        _onReady = onReady;
        new Thread(()->{
            try{
                StringBuilder message = new StringBuilder();
                byte[] data = new byte[50];
                DatagramPacket packet = new DatagramPacket(data, data.length);

                _serverSocket.receive(packet);

                message.append(new String(packet.getData(), StandardCharsets.UTF_8));
                while(!message.toString().contains(_endOfMessage)){
                    message.append(new String(data, StandardCharsets.UTF_8));
                    data = new byte[50];
                    packet = new DatagramPacket(data, data.length);

                    _serverSocket.receive(packet);
                }

                String ip = packet.getAddress().toString().replace("/", "");
                if (_blackList.contains(ip)){
                    return;
                }
                Socket socket = new Socket(ip, 23399);
                OutputStream connection = socket.getOutputStream();

                StringBuilder response = new StringBuilder();
                response.append("IP_");
                response.append(_endOfMessage);
                connection.write(response.toString().getBytes(StandardCharsets.UTF_8));

                _onReady.GetIp(ip);
            }
            catch(Exception e){
                ServerSelectionActivity.ForDebugging += e.getMessage();
            }
        }).start();

    }
    private String RemoveEndOfMessage(String data){
        return data.replace(_endOfMessage, "");
    }

    public void BlackListIp(String ip){
        _blackList.add(ip);
    }
}
