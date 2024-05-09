package com.example.PictureSender;

import android.os.Bundle;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;


public class WaitForDesktopSignal extends Fragment {



    private ListenerSocket _listener;
    private OnImageCommandReceived _onCommandReceived;

    public WaitForDesktopSignal() {
        // Required empty public constructor
    }

    public WaitForDesktopSignal(OnImageCommandReceived callBack){
        _onCommandReceived = callBack;
    }



    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        CreateListenerSocket();
        _listener.Start();
    }

    private OnImageCommandReceived CreateOnImageCallBack(){
        return new OnImageCommandReceived() {
            @Override
            public void OnCommandReceived() {
                _onCommandReceived.OnCommandReceived();
            }
        };
    }


    private void CreateListenerSocket(){
        try{
            Thread socketCreation = new Thread(()->{
                _listener = new ListenerSocket(CreateOnImageCallBack());
            });
            socketCreation.start();
            socketCreation.join();
        }
        catch(Exception e){}

    }



}