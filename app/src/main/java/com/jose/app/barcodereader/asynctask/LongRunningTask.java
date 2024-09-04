package com.jose.app.barcodereader.asynctask;

import com.google.gson.Gson;
import com.jose.app.barcodereader.model.BodyEntity;

import java.io.IOException;
import java.util.concurrent.Callable;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class LongRunningTask implements Callable<String> {
    private String mJsonData;
    private OkHttpClient mClient;

    public LongRunningTask(String jsonData) {
        this.mClient = new OkHttpClient();
        this.mJsonData = jsonData;
    }

    @Override
    public String call() throws Exception {
        RequestBody reqBody = RequestBody.create(mJsonData, MediaType.get("application/json; charset=utf-8"));
        Request request = new Request.Builder()
                .url("http://198.74.49.198/ERP/dispatch.php")
                .post(reqBody)
                .build();
        try {
            Response resp = mClient.newCall(request).execute();
            ResponseBody responseBody = resp.body();
            Gson gson = new Gson();
            BodyEntity bodyData = gson.fromJson(responseBody.string(), BodyEntity.class);
            return bodyData.getMessage();
        } catch (IOException e) {
            return "Error";
        }
    }
}
