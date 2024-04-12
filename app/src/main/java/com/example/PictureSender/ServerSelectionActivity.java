package com.example.PictureSender;

import android.Manifest;
import android.app.Activity;
import android.content.*;
import android.content.pm.PackageManager;
import android.net.MacAddress;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.*;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContract;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import android.net.wifi.p2p.WifiP2pManager.Channel;
import androidx.core.app.ActivityCompat;
import androidx.core.app.ActivityOptionsCompat;
import org.jetbrains.annotations.NotNull;

import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;


public class ServerSelectionActivity extends AppCompatActivity{

    private TextView _debug;

    private boolean _highPermission;

    private static final int MULTIPLE_REQUESTS = 15;


    private Button button;

    private ListenerSocket _listenerSocket;
    public static String ForDebugging = "debugs";

    WifiManager wm = null;
    WifiManager.MulticastLock multicastLock = null;

    public static final int SELECTION_CODE = 4;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);



        _highPermission = CheckCorrectPermissions();

        setContentView(R.layout.activity_server_selection);

        _debug = findViewById(R.id.DebugText);
        _debug.setText("");
        button = findViewById(R.id.button);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                _debug.setText(ForDebugging);
            }

        });



        CheckPermissions();
        _listenerSocket = new ListenerSocket(23499, ";;;");
        StartListening(this);
    }
    private boolean CheckCorrectPermissions(){
        return Build.VERSION.SDK_INT > Build.VERSION_CODES.S;
    }


    private void ShowPrompt(Context context, String ip){

        ((Activity) context).runOnUiThread(new Runnable() {
            @Override
                    public void run(){
                DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which){
                            case DialogInterface.BUTTON_POSITIVE:
                                Intent returnIntent = new Intent();
                                returnIntent.putExtra("ip", ip);

                                setResult(MainActivity.RESULT_OK, returnIntent);
                                finish();
                                break;

                            case DialogInterface.BUTTON_NEGATIVE:
                                _listenerSocket.BlackListIp(ip);
                                break;
                        }
                    }
                };
                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                builder.setMessage("do you want to connect to : " + ip).setPositiveButton("Yes", dialogClickListener)
                        .setNegativeButton("No", dialogClickListener).show();
            }

        });
    }
    private void CheckPermissions() {
        if (NeedsHighPermission()){
            if (LacksPermission(new String[] {Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.NEARBY_WIFI_DEVICES})){
                    LaunchMultiplePermissions();
                    return;
            }

        }

        else{
            if (LacksPermission(Manifest.permission.ACCESS_FINE_LOCATION)) {
                ActivityResultLauncher<String> requestPermissionLauncher = CreatePermissionRequest();
                requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
                return;
            }
        }
    }
    private ActivityResultLauncher<String> CreatePermissionRequest(){
        return registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
            if (isGranted) {

            }
        });
    }

    private boolean NeedsHighPermission(){
        return _highPermission;
    }


    private void LaunchMultiplePermissions(){
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.NEARBY_WIFI_DEVICES,
            Manifest.permission.ACCESS_FINE_LOCATION}, MULTIPLE_REQUESTS);
    }


    private boolean LacksPermission(String perm){
        return ActivityCompat.checkSelfPermission(this, perm) != PackageManager.PERMISSION_GRANTED;
    }

    private boolean LacksPermission(String[] perms){
        for (String perm:perms){
            if (ActivityCompat.checkSelfPermission(this, perm) != PackageManager.PERMISSION_GRANTED){
                return true;
            }
        }
        return false;
    }



    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
         if (!CheckMultipleRequests(requestCode, grantResults)){
             _debug.setText("Not sufficient permissions");
             return;
         }
    }

    private boolean CheckMultipleRequests(int requestID, int[] grantResults){
        if (requestID == MULTIPLE_REQUESTS){
            for (int i = 0; i < grantResults.length; i++){
                if (grantResults[i] != PackageManager.PERMISSION_GRANTED){
                    return false;
                }
            }
        }
        return true;
    }

    private void StartListening(Context context){
        _listenerSocket.StartListening(new GetIpCallBack() {
            @Override
            public void GetIp(String ip4) {
                ShowPrompt(context, ip4);
            }
        });
    }



    @Override
    protected void onResume() {
        super.onResume();
        if(wm == null)wm = (WifiManager)getSystemService(Context.WIFI_SERVICE);
        if(multicastLock == null){
            multicastLock = wm.createMulticastLock("stackoverflow for the win");
            multicastLock.setReferenceCounted(true);
        }
        if(multicastLock != null && !multicastLock.isHeld()) multicastLock.acquire();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(multicastLock != null && multicastLock.isHeld()) multicastLock.release();
    }

}

