package top.sshh.bililiverecoder.util.bili;

import com.alibaba.fastjson.JSONObject;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;

public class WebCookie extends Cookie {
    private static final long serialVersionUID = 2168152194164783950L;


    /**
     * DedeUserID=(登录mid)&
     * DedeUserID__ckMd5=(登录mid MD5值)&
     * Expires=(过期时间 秒)&
     * SESSDATA=(登录token)&
     * bili_jct=(csrf)&
     * gourl=(跳转网址 默认为主页)
     */

    private final String DedeUserID = "DedeUserID";
    private final String DedeUserID__ckMd5 = "DedeUserID__ckMd5";
    private final String Expires = "Expires";
    private final String SESSDATA = "SESSDATA";
    private final String bili_jct = "bili_jct";


    @Override
    public String put(String key, String value) {
        return super.put(key, value);
    }

    public String getDedeUserID() {
        return get(DedeUserID);
    }

    public void setDedeUserID(String dedeUserID) {
        put(DedeUserID, dedeUserID);
    }

    public String getDedeUserID__ckMd5() {
        return get(DedeUserID__ckMd5);
    }

    public void setDedeUserID__ckMd5(String dedeUserID__ckMd5) {
        put(DedeUserID__ckMd5, dedeUserID__ckMd5);
    }

    public String getExpires() {
        return get(Expires);
    }

    public void setExpires(String expires) {
        put(Expires, expires);
    }

    public String getSESSDATA() {
        return get(SESSDATA);
    }

    public void setSESSDATA(String SESSDATA) {
        put(SESSDATA, SESSDATA);
    }

    public String getBili_jct() {
        return get(bili_jct);
    }

    public void setBili_jct(String bili_jct) {
        put(bili_jct, bili_jct);
    }

    public String getCookie() {
        StringBuffer stringBuffer = new StringBuffer();
        this.forEach((s, s2) -> stringBuffer.append(s).append("=").append(s2).append(";"));
        return stringBuffer.toString();
    }

    /**
     * 包含cookie的url
     *
     * @param cookie 从游戏分站跨域登录url分析出cookie
     */
    public void setCookie(URL cookie) {
        truncateUrlPage(cookie.toString());
    }

    /**
     * 截取URL中的？之后的部分
     *
     * @param strURL
     * @return
     */
    private void truncateUrlPage(String strURL) {
        String strAllParam = null;
        String[] arrSplit = null;

        strURL = strURL.trim();
        //截取URL中的？之后的部分
        arrSplit = strURL.split("[?]");
        if (strURL.length() > 1) {
            if (arrSplit.length > 1) {
                if (arrSplit[1] != null) {
                    strAllParam = arrSplit[1];
                }
            }
        }
        //解析每个参数，并放入map中
        String[] param = strAllParam.split("&");
        for (String s : param) {
            String[] split = s.split("=", 2);
            if (split.length > 1) {
                put(split[0].trim(), split[1].trim());
            }
        }

    }

    @Override
    public String toString() {
        return super.toString();
    }

    @Override
    public JSONObject toJSONString() {
        return super.toJSONString();
    }

    @Override
    public void writeCookieToJson(String filePath) throws Exception {
        super.writeCookieToJson(filePath);
    }

    @Override
    public void serializable(String filePath) throws IOException {
        super.serializable(filePath);
    }

    @Override
    public void setCookieToHeader(HashMap<String, String> header) {
        super.setCookieToHeader(header);
    }
}
