package com.piyush.samplemultipart;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {

    private Thread thread;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button button = findViewById(R.id.btn);
        thread = new Thread(new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < 100; i++)
                    sendMultipart();
            }
        });
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                thread.start();
            }
        });
    }

    private void sendMultipart() {
        Apis api = ApiClient.getClient().create(Apis.class);
        File file = createFile();
        Log.e("check_file", file.getAbsolutePath());
        RequestBody reqFile = RequestBody.create(MediaType.parse("multipart/form-data"), file);
        MultipartBody.Part body = MultipartBody.Part.createFormData("file", file.getName(), reqFile);
        final long startTime = System.currentTimeMillis();
        api.sendImage(body).enqueue(new Callback<String>() {
            @Override
            public void onResponse(@NonNull Call<String> call, @NonNull Response<String> response) {
                Log.e("check_res", response.body() + " : Time - > " + (System.currentTimeMillis() - startTime) + "ms");
            }

            @Override
            public void onFailure(@NonNull Call<String> call, @NonNull Throwable t) {
                Log.e("check_err", t.toString());
                t.printStackTrace();
            }
        });
//        Call<String> call = api.sendImage(body);
//        try {
//            String res = call.execute().body();
//            Log.e("check_res_"+count,res + " : Time - > "+(System.currentTimeMillis() - startTime)+"ms");
//            count++;
//        } catch (IOException e) {
//            e.printStackTrace();
//            Log.e("check_err",e.toString());
//            e.printStackTrace();
//        }
    }

    private File createFile() {
        File f = new File(getCacheDir(), "sample.jpeg");
        try {
            f.createNewFile();
            Bitmap bm = BitmapFactory.decodeResource(getResources(), R.drawable.img);
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            bm.compress(Bitmap.CompressFormat.JPEG, 0 /*ignored for PNG*/, bos);
            byte[] bitmapData = bos.toByteArray();

            //write the bytes in file
            FileOutputStream fos = new FileOutputStream(f);
            fos.write(bitmapData);
            fos.flush();
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return f;
    }
}
