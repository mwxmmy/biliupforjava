package top.sshh.bililiverecoder.util.bili.user;

import com.alibaba.fastjson.JSONObject;
import org.apache.http.HttpException;
import top.sshh.bililiverecoder.util.bili.Cookie;
import top.sshh.bililiverecoder.util.bili.HttpClientResult;
import top.sshh.bililiverecoder.util.bili.HttpClientUtils;

import java.io.IOException;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;

public class UserMy {
    private static final String URL = "http://api.bilibili.com/x/space/myinfo";
    private Cookie cookie;
    private HashMap<String, String> headers = new HashMap<String, String>();

    public UserMy(Cookie cookie) {
        this.cookie = cookie;
        headers.clear();
        cookie.toHeaderCookie(headers);
    }

    public UserMyRootBean getPojo() throws HttpException {
        String page = null;
        try {
            page = getPage();
        } catch (Exception e) {
            throw new HttpException("访问URL失败", e);
        }
        if (page == null) return null;
        UserMyRootBean bean = JSONObject.parseObject(page, UserMyRootBean.class);
        return bean;
    }

    /**
     * 获取用户的信息
     *
     * @return
     * @throws IOException
     * @throws NoSuchAlgorithmException
     * @throws KeyStoreException
     * @throws URISyntaxException
     * @throws KeyManagementException
     */
    public String getPage() throws IOException, NoSuchAlgorithmException, KeyStoreException, URISyntaxException, KeyManagementException {
        HttpClientResult result = HttpClientUtils.doGet(URL, headers, null);
        return result.getContent();
    }

    public Cookie getCookie() {
        return cookie;
    }

    public void setCookie(Cookie cookie) {
        this.cookie = cookie;
        headers.clear();
        cookie.toHeaderCookie(headers);
    }
}
