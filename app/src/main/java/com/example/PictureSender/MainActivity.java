package com.example.PictureSender;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ExifInterface;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import android.view.View;
import android.widget.ImageView;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;


public class MainActivity extends AppCompatActivity {



    public static int CAMERACODE = 1;
    private final String _endMessage = ";;;";
    private ClientSocket _client;

    private Intent _cameraActivity;

    private Bitmap _bmp;

    private ImageView _openCameraImage;

    private String _ip;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        StartServerSearchActivity();

        setContentView(R.layout.activity_main);


        _openCameraImage = findViewById(R.id.CameraImage);

        //Define and attach click listener
        _openCameraImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                StartImageCapture();
            }
        });

        _client = new ClientSocket(_endMessage);


    }

    private void StartServerSearchActivity(){
        runOnUiThread(()->{
                Intent serverSearch = new Intent(MainActivity.this, ServerSelectionActivity.class);
                startActivityForResult(serverSearch, ServerSelectionActivity.SELECTION_CODE);
        });

    }


    private void StartImageCapture() {

        try{
            _openCameraImage.setEnabled(false);
            _cameraActivity = new Intent(this, CameraActivation.class);
            startActivityForResult(_cameraActivity, CAMERACODE);

        }
        catch(Exception e){
            ShowNotificationWithOK("Failed to start camera");
        }






    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CAMERACODE && resultCode == Activity.RESULT_OK) {

            SendImage(data);
            StartServerSearchActivity();
        }

        if (requestCode == CAMERACODE && resultCode == Activity.RESULT_CANCELED){
            _openCameraImage.setEnabled(true);
        }
        if (requestCode == ServerSelectionActivity.SELECTION_CODE & resultCode == Activity.RESULT_OK){
            _ip = data.getStringExtra("ip");
            EstablishConnection();
            SendHandshake();
            StartPolling();
        }





    }

    private void EstablishConnection(){

            Thread thread = new Thread(()->{
                try {
                    _client.StartConnection(_ip, 23399);
                }
                catch(Exception e){
                    ShowNotificationWithOK("Failed to connect");
                }
            });
            try{
                thread.start();
                thread.join();
            }
            catch(Exception e){
                ShowNotificationWithOK("Failed to start connection");
            }



    }

    private void ShowNotificationWithOK(String text){
        runOnUiThread(()->{
            AlertDialog.Builder dialog = new AlertDialog.Builder(MainActivity.this);
            dialog.setMessage(text);
            dialog.setPositiveButton("OK", null);
            dialog.show();
        });
    }

    private void ShowNotificationWithOK(String text, DialogInterface.OnClickListener listener){
        runOnUiThread(()->{
            AlertDialog.Builder dialog = new AlertDialog.Builder(this);
            dialog.setMessage(text);
            dialog.setPositiveButton("OK", listener);
            dialog.show();
        });
    }
    private void SendHandshake(){
        new Thread(() ->{
            try{
                _client.SendMessage("IP_".getBytes(StandardCharsets.UTF_8));
            }
            catch(Exception e){
                ShowNotificationWithOK("Failed to execute handshake");
            }
        }).start();
    }
    private void StartPolling(){
        try{

            new Thread(() -> {
                try{
                    while (_client.SendMessage(new byte[0])){
                            try {
                                Thread.sleep(5000L);
                            }
                            catch(Exception e){
                                ShowNotificationWithOK("Failed to upkeep connection");
                            }
                    }
                    ShowNotificationWithOK("Connection failed");
                }
                catch(Exception e){
                   ShowNotificationWithOK("Connection to server has closed",
                           CreateOnClickForListeningToConnections());
                }
            }).start();
        }
        catch(Exception e){
            ShowNotificationWithOK("Failed to start connection");
        }
    }
    private DialogInterface.OnClickListener CreateOnClickForListeningToConnections(){
        return new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                try{
                    _client.Close();
                }
                catch(Exception e){}
                StartServerSearchActivity();
            }
        };
    }
    private void SendImage(Intent data){
        try{

            String path = data.getStringExtra("file");

            _openCameraImage.setEnabled(false);
            Thread thread = new Thread(() -> {
                try {
                    byte[] imgTag = "IMG_".getBytes(StandardCharsets.UTF_8);
                    byte[] orientationData = new byte[] {(byte)GetImageOrientationBit(path)};
                    byte[] orientationTag = "WO_".getBytes();
                    ByteArrayOutputStream imageStream = CompressImage(path);
                    byte[] imageData = imageStream.toByteArray();
                    int destPos = 0;
                    byte[] combinedData = new byte[imgTag.length + orientationData.length
                            + imageData.length + orientationTag.length];

                    System.arraycopy(imgTag, 0, combinedData, destPos, imgTag.length);
                    destPos += imgTag.length;

                    System.arraycopy(orientationTag, 0, combinedData, destPos, orientationTag.length);
                    destPos += orientationTag.length;

                    System.arraycopy(orientationData, 0, combinedData, destPos, orientationData.length);
                    destPos += orientationData.length;

                    System.arraycopy(imageData, 0, combinedData, destPos, imageData.length);


                    if (!_client.SendMessage(combinedData)){
                        ShowNotificationWithOK("failed to send Image");
                    }


                    _bmp.recycle();
                    ((Activity) MainActivity.this).runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            _openCameraImage.setEnabled(true);
                        }
                    });

                }
                catch (Exception e)
                {
                    ShowNotificationWithOK("Something went wrong with creating/sending image");
                }
            });
            try{
                thread.start();


            }
            catch(Exception e){
                ShowNotificationWithOK("Failed to start sending sequence");
            }
        }
        catch(Exception e){
            ShowNotificationWithOK("Failed to start sending sequence");
        }
    }

    private int GetImageOrientationBit(String path){
        try{
            ExifInterface exif = new ExifInterface(path);
            String orientation = exif.getAttribute(ExifInterface.TAG_ORIENTATION);

            return Integer.parseInt(orientation);
        }
        catch(Exception e){

        }
        return -1;

    }
    private ByteArrayOutputStream CompressImage(String path){
        _bmp = BitmapFactory.decodeFile(path);

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        _bmp.compress(Bitmap.CompressFormat.JPEG, 100, stream);
        return stream;
    }




}