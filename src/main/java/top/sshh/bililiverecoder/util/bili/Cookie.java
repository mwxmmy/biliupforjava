package top.sshh.bililiverecoder.util.bili;

import com.alibaba.fastjson.JSONObject;
import top.sshh.bililiverecoder.util.JsonSerializeUtil;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.function.BiConsumer;


/**
 * 所有Cookie的父类，Cookie以键值对的形式进行存储
 * 任何cookie都可以被父类存储，子类则可以对一个已确定的cookie进行操作
 */
public abstract class Cookie extends HashMap<String, String> implements Serializable {
    private static final long serialVersionUID = 2168152194164783950L;

    /**
     * 将字符串解析出Cookie
     *
     * @param cookie
     * @return
     */
    public static WebCookie parse(String cookie) {
        WebCookie webCookie = new WebCookie();
        for (String cookie0 : cookie.trim().split(";")) {
            String[] split = cookie0.split("=");
            if (split.length < 2) {
                split = cookie0.split(":");
                if (split.length == 2) {
                    webCookie.put(split[0].trim(), split[1].trim());
                }
                continue;
            }
            webCookie.put(split[0].trim(), split[1].trim());
        }
        return webCookie;
    }

    /**
     * 读取一个在硬盘上的Cookie JSON 文件
     *
     * @param filePath
     * @return
     */
    public static Cookie readCookie(String filePath) throws IOException {
        Cookie cookie = new Cookie() {
        };
        JSONObject jsonObject = JsonSerializeUtil.readToJSONObject(filePath);
        jsonObject.forEach(new BiConsumer<String, Object>() {
            @Override
            public void accept(String key, Object value) {
                cookie.put(key, value.toString());
            }
        });
        return cookie;
    }

    /**
     * 序列化到本地
     *
     * @param filePath
     */
    public static Cookie readSerializable(String filePath) throws IOException, ClassNotFoundException {
        ObjectInputStream objectInputStream = null;
        try {
            objectInputStream = new ObjectInputStream(new FileInputStream(filePath));
            Cookie cookie = (Cookie) objectInputStream.readObject();
            return cookie;
        } finally {
            if (objectInputStream != null) {
                objectInputStream.close();
            }
        }

        //        return null;  删除可用： throws IOException, ClassNotFoundException
    }

    /**
     * 将一个Cookie持久化到一个JSON文件
     *
     * @param cookie
     * @param filePath
     * @throws Exception
     */
    public static void writeCookie(Cookie cookie, String filePath) throws Exception {
        JsonSerializeUtil.write(cookie.toJSONString(), filePath);
    }

    /**
     * 序列化到本地
     *
     * @param filePath
     */
    public static void writeSerializable(Cookie cookie, String filePath) throws IOException {
        ObjectOutputStream objectOutputStream = null;
        try {
            objectOutputStream = new ObjectOutputStream(Files.newOutputStream(Paths.get(filePath + ".cookie")));
            objectOutputStream.writeObject(cookie);
        } finally {
            if (objectOutputStream != null) {
                objectOutputStream.close();
            }
        }
    }

    public String getToken() {
        return null;
    }

    public String getCsrf() {
        return this.get("bili_jct");
    }

    @Override
    public String toString() {
        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append("Cookie:{\r\n\t");
        for (String key : keySet()) {
            stringBuffer.append(key).append("=").append(get(key)).append(";").append("\r\n\t");
        }
        stringBuffer.append("\r\n");
        stringBuffer.append("}");
        return stringBuffer.toString();
    }

    public String toStringV2() {
        StringBuffer stringBuffer = new StringBuffer();
        for (String key : keySet()) {
            stringBuffer.append(key).append("=").append(get(key)).append(";");
        }
        return stringBuffer.toString();
    }

    public JSONObject toJSONString() {
        JSONObject jsonObject = new JSONObject();
        for (String key : keySet()) {
            jsonObject.put(key, get(key));
        }
        return jsonObject;
    }

    /**
     * 将一个Cookie持久化
     *
     * @param filePath
     * @throws Exception
     */
    public void writeCookieToJson(String filePath) throws Exception {
        JsonSerializeUtil.write(toJSONString(), filePath);
    }

    /**
     * 序列化到本地
     *
     * @param filePath
     */
    public void serializable(String filePath) throws IOException {
        ObjectOutputStream objectOutputStream = null;
        try {
            objectOutputStream = new ObjectOutputStream(Files.newOutputStream(Paths.get(filePath + ".cookie")));
            objectOutputStream.writeObject(this);
        } finally {
            if (objectOutputStream != null) {
                objectOutputStream.close();
            }
        }
    }


    /**
     * 将cookie设置到header的map里面
     *
     * @param header
     */
    @Deprecated
    public void setCookieToHeaderOld(HashMap<String, String> header) {
        header.put("Cookie", toString());
    }


    /**
     * 填装Cookie到请求头
     */
    @Deprecated
    public HashMap<String, String> toHeaderCookieOld(HashMap<String, String> map) {
        map.put("Cookie", toString());
        return map;
    }

    /**
     * 填装Cookie到请求头
     */
    @Deprecated
    public HashMap<String, String> toHeaderCookieOld() {
        HashMap<String, String> map = new HashMap<>();
        map.put("Cookie", toString());
        return map;
    }


    /**
     * 将cookie设置到header的map里面
     *
     * @param header
     */
    public void setCookieToHeader(HashMap<String, String> header) {
        header.put("Cookie", toStringV2());
    }


    /**
     * 填装Cookie到请求头
     */
    public HashMap<String, String> toHeaderCookie(HashMap<String, String> map) {
        map.put("Cookie", toStringV2());
        return map;
    }

    /**
     * 填装Cookie到请求头
     */
    public HashMap<String, String> toHeaderCookie() {
        HashMap<String, String> map = new HashMap<>();
        map.put("Cookie", toStringV2());
        return map;
    }


    /**
     * 刷新Cookie，将访问 www.bilibili.com 以此来获取最新的cookie验证，否则部分场景将会禁止访问
     */
    public void updateCookie() throws IOException {
        HttpClientResult result = HttpClientUtils.doGet("http://www.bilibili.com");
        for (org.apache.http.cookie.Cookie cookie : result.getCookie().getCookies()) {
            String key = cookie.getName();
            String value = cookie.getValue();
            this.put(key, value);
        }
    }

    @Override
    public boolean isEmpty() {
        return super.isEmpty() || !this.containsKey("SESSDATA");
    }
}
