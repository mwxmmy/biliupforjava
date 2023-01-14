package top.sshh.bililiverecoder.util;

import lombok.extern.slf4j.Slf4j;
import okhttp3.*;

import java.io.IOException;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
public class HttpClientUtil {
    private static OkHttpClient client;
    private static OkHttpClient clientAllowCookie;

    static {
        HttpsTrustManager.allowAllSSL();
        HttpsTrustManager manager = new HttpsTrustManager();
        client = new OkHttpClient().newBuilder()
                .sslSocketFactory(HttpsTrustManager.createSSLSocketFactory(),manager)
                .connectTimeout(150, TimeUnit.SECONDS)
                .readTimeout(150, TimeUnit.SECONDS)
                .protocols(Collections.singletonList(Protocol.HTTP_1_1))
                .writeTimeout(150, TimeUnit.SECONDS)
                .build();

        CookieManager cookieManager = new CookieManager();
        cookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ALL);
        CookieJar cookieJar = new JavaNetCookieJar(cookieManager);
        clientAllowCookie = new OkHttpClient().newBuilder()
                .sslSocketFactory(HttpsTrustManager.createSSLSocketFactory(),manager)
                .connectTimeout(150, TimeUnit.SECONDS)
                .readTimeout(150, TimeUnit.SECONDS)
                .protocols(Collections.singletonList(Protocol.HTTP_1_1))
                .writeTimeout(150, TimeUnit.SECONDS)
                .cookieJar(cookieJar)
                .build();
    }

    public static String post(String url, Map<String, String> headers, String json) {
        try {
            RequestBody requestBody = RequestBody.create(MediaType.parse("application/json"), json);
            Request build = new Request.Builder()
                    .headers(Headers.of(headers))
                    .url(url)
                    .post(requestBody)
                    .build();
            Response response = client.newCall(build).execute();
            String string = response.body().string();
            return string;
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage());
        }
    }
    public static String post(String url, Map<String, String> headers, RequestBody requestBody) {
        try {
            Request build = new Request.Builder()
                    .headers(Headers.of(headers))
                    .url(url)
                    .post(requestBody)
                    .build();
            Response response = client.newCall(build).execute();
            String string = response.body().string();
            return string;
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage());
        }
    }

    public static String post(String url, Map<String, String> headers,
                              Map<String, String> formParams,
                              Boolean allowCookie) {
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
        try {

            Response response = currentClient.newCall(build).execute();
            String string = response.body().string();
            return string;
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage());
        }
    }

    public static String get(String url, Map<String, String> headers) {
        String string;
        do {
            try {
                Response response = client.newCall(new Request.Builder()
                        .url(url)
                        .headers(Headers.of(headers))
                        .get()
                        .build()
                ).execute();
                string = response.body().string();
                return string;
            } catch (UnknownHostException e) {
                try {
                    Thread.sleep(5000L);
                } catch (Exception ignored) {
                }

            } catch (Exception e) {
                e.printStackTrace();
                throw new RuntimeException(e.getMessage());
            }
        } while (true);

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
            if (v instanceof String) {
                builder.addFormDataPart(k, (String) v);
            } else {
                builder.addFormDataPart(k, "file", (RequestBody)v);
            }
        });

        Request.Builder post = new Request.Builder()
                .url(url)
                .post(builder.build());
        headers.forEach(post::header);
        Request request = post
                .build();
        String string = clientAllowCookie.newCall(request).execute().body().string();
        return string;
    }


    public static OkHttpClient getClient() {
        return client;
    }


}
