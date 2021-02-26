package com.soapdemo.rxapp;

import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.gson.Gson;
import com.orhanobut.logger.Logger;
import com.soapdemo.rxapp.models.Photo;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Single;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class MainActivity extends AppCompatActivity {
    TextView textView;
    ImageView imageView;
    private static final OkHttpClient client = new OkHttpClient();
    private String url = "https://api.unsplash.com/photos/random?client_id=ki5iNzD7hebsr-d8qUlEJIhG5wxGwikU71nsqj8PcMM";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        textView = this.findViewById((R.id.msg_view));
        imageView = this.findViewById(R.id.imageView);
        this.findViewById(R.id.button).setOnClickListener( v -> {
            Request request = new Request.Builder()
                    .url(url)
                    .get()
                    .build();
            Call call = client.newCall(request);
            this.textView.setText( "Finding...." );
            HttpJsonObservable(call,Photo.class)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe( t ->{
                        this.textView.setText(t.alt_description);
                        Request photoRequest = new Request.Builder()
                                .url(t.urls.small)
                                .get()
                                .build();
                        Call photoCall = client.newCall(photoRequest);
                        this.HttpStreamObservable(photoCall)
                                .observeOn( AndroidSchedulers.mainThread())
                                .subscribe( s-> {
                                    Bitmap bitmap = BitmapFactory.decodeStream(s);
                                    s.close();
                                    imageView.setImageBitmap(bitmap);
                                },
                                e -> textView.setText( e.getMessage() )
                                );
                    },
                error -> this.textView.setText(error.getMessage())
            );
        } );
    }

    private  Single<InputStream> HttpStreamObservable(Call call )
    {
        return Single.create(subscriber -> {
            //Logger.i( "------Begin To Request Http Thread:%s",  Thread.currentThread().getId() );
            call.enqueue(new Callback() {
                @Override
                public void onFailure(@NotNull Call call, @NotNull IOException e) {
                    Logger.i("------- Http Error:%s",  e.getMessage() );
                    subscriber.onError( e );
                }

                @Override
                public void onResponse(@NotNull Call call, @NotNull Response response) {
                    Logger.i("------- Http Response Thread:%s",  Thread.currentThread().getId() );
                    try{
                        if( response.isSuccessful() ) {
                            InputStream stream = response.body().byteStream();
                            subscriber.onSuccess(stream);
                        }
                        else
                        {
                            subscriber.onError( new Exception( String.format( "error state code: %s", response.code() ) ) );
                        }
                    }
                    catch(Exception exception) {
                        response.close();
                    }
                }
            });
        });
    }

    private <T> Single<T> HttpJsonObservable( Call call , Class<T> classOfT )
    {
        return Single.create(subscriber -> {
            //Logger.i( "------Begin To Request Http Thread:%s",  Thread.currentThread().getId() );
            call.enqueue(new Callback() {
                @Override
                public void onFailure(@NotNull Call call, @NotNull IOException e) {
                    Logger.i("------- Http Error:%s",  e.getMessage() );
                    subscriber.onError( e );
                }

                @Override
                public void onResponse(@NotNull Call call, @NotNull Response response) {
                    Logger.i("------- Http Response Thread:%s",  Thread.currentThread().getId() );
                    try (ResponseBody body = response.body()){
                        if( response.isSuccessful() ) {
                            try {
                                String json = body.string();
                                Gson gson = new Gson();
                                T jsonObj = gson.fromJson(json,classOfT );
                                subscriber.onSuccess(jsonObj);
                            } catch (IOException e) {
                                subscriber.onError(e);
                            }
                        }
                        else
                        {
                            subscriber.onError( new Exception( String.format( "error state code: %s", response.code() ) ) );
                        }
                    }
                }
            });
        });
    }

}