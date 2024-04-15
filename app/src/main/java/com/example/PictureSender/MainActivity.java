package com.example.PictureSender;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ExifInterface;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;


public class MainActivity extends AppCompatActivity {


    private TextView _errors;
    public static int CAMERACODE = 1;
    private final String _endMessage = ";;;";
    private ClientSocket _client;
    int length = 0;
    private Intent _cameraActivity;

    public static String test = "test";

    private Bitmap _bmp;

    private ImageView _openCameraImage;

    private String _ip;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent serverSearch = new Intent(this, ServerSelectionActivity.class);
        startActivityForResult(serverSearch, ServerSelectionActivity.SELECTION_CODE);

        setContentView(R.layout.activity_main);


        _errors = findViewById(R.id.ErrorText);
        _errors.setText("");

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

    private void StartImageCapture() {

        try{
            _openCameraImage.setEnabled(false);
            _cameraActivity = new Intent(this, CameraActivation.class);
            startActivityForResult(_cameraActivity, CAMERACODE);

        }
        catch(Exception e){
            _errors.setText(e.getMessage());
        }






    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == CAMERACODE && resultCode == Activity.RESULT_OK) {

            SendImage(data);

        }

        if (requestCode == CAMERACODE && resultCode == Activity.RESULT_CANCELED){
            _openCameraImage.setEnabled(true);
        }
        if (requestCode == ServerSelectionActivity.SELECTION_CODE & resultCode == Activity.RESULT_OK){
            _ip = data.getStringExtra("ip");
            EstablishConnection();
            _errors.setText(_errors.getText() + "going to start handshake");
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
                    _errors.setText("Failed to start connection");
                }
            });
            try{
                thread.start();

                thread.join();
                _errors.setText("established connection");
            }
            catch(Exception e){

            }



    }

    private void SendHandshake(){
        new Thread(() ->{
            try{
                runOnUiThread(()-> {_errors.setText("Sending handshake");});
                boolean b = _client.SendMessage("IP_".getBytes(StandardCharsets.UTF_8));
                runOnUiThread(()-> {_errors.setText(String.valueOf(b));});
            }
            catch(Exception e){
                _errors.setText("something wrong in handshake");
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
                                _errors.setText("sleep failed");
                            }
                    }
                    runOnUiThread(()-> {_errors.setText("sending message failed");});
                }
                catch(Exception e){
                    runOnUiThread(()-> {_errors.setText("Could not connect to server");});
                }
            }).start();
        }
        catch(Exception e){
            _errors.setText("Something went wrong in starting thread");
        }
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
                        _errors.setText("Sending image failed");
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
                    _errors.setText(e.toString() + "exception1");
                }
            });
            try{
                thread.start();


            }
            catch(Exception e){
                _errors.setText(e.getMessage());
            }
        }
        catch(Exception e){
            _errors.setText(e.toString() + " exception2");
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