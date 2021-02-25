package com.soapdemo.rxapp;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.widget.TextView;

import com.google.gson.Gson;
import com.orhanobut.logger.Logger;
import com.soapdemo.rxapp.models.Photo;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Observable;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class MainActivity extends AppCompatActivity {
    TextView textView;
    private static final OkHttpClient client = new OkHttpClient();
    private String url = "https://api.unsplash.com/photos/random?client_id=ki5iNzD7hebsr-d8qUlEJIhG5wxGwikU71nsqj8PcMM";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        textView = this.findViewById((R.id.msg_view));
        this.findViewById(R.id.button).setOnClickListener( v -> {
            Request request = new Request.Builder()
                    .url(url)
                    .get()
                    .build();
            Call call = client.newCall(request);
            HttpJsonObservable(call,Photo.class)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe( t -> this.textView.setText(t.alt_description),
                        error -> this.textView.setText(error.getMessage())
            );
        } );
    }

    private <T> Observable<T> HttpJsonObservable( Call call , Class<T> classOfT )
    {
        return Observable.create(subscriber -> {
            Logger.i( "------Begin To Request Http Thread:%s",  Thread.currentThread().getId() );
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
                                subscriber.onNext(jsonObj);
                                subscriber.onComplete();
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