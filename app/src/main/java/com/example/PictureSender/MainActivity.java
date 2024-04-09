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


public class MainActivity extends AppCompatActivity {


    private TextView _text;
    public static int CAMERACODE = 1;
    private String _endMessage = ";;;";
    private ClientSocket _client;
    int length = 0;
    private Intent _cameraActivity;

    public static String test = "test";

    private Bitmap _bmp;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        _text = findViewById(R.id.textView);

        ImageView droid = findViewById(R.id.droidImage);

        //Define and attach click listener
        droid.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                tapDroid();
            }
        });

        _client = new ClientSocket();


    }

    private void tapDroid() {

        try{
            _cameraActivity = new Intent(this, CameraActivity.class);
            startActivityForResult(_cameraActivity, CAMERACODE);

        }
        catch(Exception e){
            _text.setText(e.getMessage());
        }






    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == CAMERACODE && resultCode == Activity.RESULT_OK) {
            try{

                String path = data.getStringExtra("file");


                Thread thread = new Thread(() -> {
                    try {

                        byte[] orientationData = new byte[] {(byte)GetImageOrientationBit(path)};
                        byte[] orientationTag = "WO_".getBytes();
                        ByteArrayOutputStream imageStream = CompressImage(path);
                        byte[] imageData = imageStream.toByteArray();
                        int destPos = 0;
                        byte[] combinedData = new byte[orientationData.length + imageData.length + orientationTag.length];

                        System.arraycopy(orientationTag, 0, combinedData, destPos, orientationTag.length);
                        destPos += orientationTag.length;

                        System.arraycopy(orientationData, 0, combinedData, destPos, orientationData.length);
                        destPos += orientationData.length;

                        System.arraycopy(imageData, 0, combinedData, destPos, imageData.length);


                        _client.startConnection("192.168.1.103", 77);
                        _client.sendMessage(combinedData, _endMessage);

                        _client.stopConnection();
                        _bmp.recycle();


                    } catch (Exception e)
                    {
                        _text.setText(e.toString() + "exception1");
                    }
                });
                try{
                    thread.start();
                }
                catch(Exception e){
                    _text.setText(e.getMessage());
                }




            }
            catch(Exception e){
                _text.setText(e.toString() + " exception2");
            }

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