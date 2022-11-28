package top.sshh.bililiverecoder.util;

import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;

import java.io.IOException;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
public class HttpClientUtil {
    private static OkHttpClient client;
    private static OkHttpClient clientAllowCookie;

    static {
        client = new OkHttpClient().newBuilder()
                .connectTimeout(150, TimeUnit.SECONDS)
                .readTimeout(150, TimeUnit.SECONDS)
                .protocols(Collections.singletonList(Protocol.HTTP_1_1))
                .writeTimeout(150, TimeUnit.SECONDS)
                .build();

        CookieManager cookieManager = new CookieManager();
        cookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ALL);
        CookieJar cookieJar = new JavaNetCookieJar(cookieManager);
        clientAllowCookie = new OkHttpClient().newBuilder()
                .connectTimeout(150, TimeUnit.SECONDS)
                .readTimeout(150, TimeUnit.SECONDS)
                .protocols(Collections.singletonList(Protocol.HTTP_1_1))
                .writeTimeout(150, TimeUnit.SECONDS)
                .cookieJar(cookieJar)
                .build();
    }

    public static String post(String url, Map<String, String> headers, String json) throws IOException {
        RequestBody requestBody = RequestBody.create(MediaType.parse("application/json"), json);
        Request build = new Request.Builder()
                .headers(Headers.of(headers))
                .url(url)
                .post(requestBody)
                .build();
        Response response = client.newCall(build).execute();
        String string = response.body().string();
        log.info("url={}, header={}, param={}, resp={}", url, JSON.toJSONString(headers), json, string);
        return string;
    }

    public static String post(String url, Map<String, String> headers,
                              Map<String, String> formParams,
                              Boolean allowCookie) throws IOException {
        FormBody.Builder builder = new FormBody.Builder();
        formParams.forEach(builder::add);
        RequestBody formBody = builder
                .build();
        Request build = new Request.Builder()
                .headers(Headers.of(headers))
                .url(url)
                .post(formBody)
                .build();
        OkHttpClient currentClient = allowCookie ? clientAllowCookie : client;
        Response response = currentClient.newCall(build).execute();
        String string = response.body().string();
        log.info("url={}, header={}, param={}, resp={}", url, JSON.toJSONString(headers), formParams, string);
        return string;
    }

    public static String get(String url, Map<String, String> headers) throws IOException {
        Response response = client.newCall(new Request.Builder()
                .url(url)
                .headers(Headers.of(headers))
                .get()
                .build()
        ).execute();
        String string = response.body().string();
        log.info("url={}, header={}, resp={}", url, JSON.toJSONString(headers), string);
        return string;
    }

    public static String get(String url) throws IOException {
        Response response = client.newCall(new Request.Builder()
                .url(url)
                .get()
                .build()
        ).execute();
        return response.body().string();
    }

    public static String upload(String url, Map<String,String> headers, Map<String, Object> params) throws IOException {
        MultipartBody.Builder builder = new MultipartBody.Builder().setType(MultipartBody.FORM);

        params.forEach((k, v) -> {
            if (v instanceof String)
                builder.addFormDataPart(k, (String) v);
            else {
                builder.addFormDataPart(k, "file", RequestBody.create((byte[]) v));
            }
        });

        Request.Builder post = new Request.Builder()
                .url(url)
                .post(builder.build());
        headers.forEach(post::header);
        Request request = post
                .build();
        String string = clientAllowCookie.newCall(request).execute().body().string();
        log.info("url={}, resp={}", url, string);
        return string;
    }


    public static OkHttpClient getClient() {
        return client;
    }


}
