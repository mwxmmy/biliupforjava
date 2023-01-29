package top.sshh.bililiverecoder.util.bili.upload;

import com.alibaba.fastjson.JSONObject;
import org.apache.http.HttpException;
import org.apache.http.entity.StringEntity;
import top.sshh.bililiverecoder.util.bili.Cookie;
import top.sshh.bililiverecoder.util.bili.HttpClientResult;
import top.sshh.bililiverecoder.util.bili.HttpClientUtils;
import top.sshh.bililiverecoder.util.bili.upload.pojo.PublishVideoBean;

import java.io.IOException;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;

/**
 * @author mwxmmy
 */
public class PublishVideoRequest {

    private static final String URL = "https://member.bilibili.com/x/vu/web/add/v3";
    private Cookie cookie;
    private final HashMap<String, String> headers = new HashMap<String, String>();

    private final String body;

    public PublishVideoRequest(Cookie cookie,String body) {
        this.cookie = cookie;
        headers.clear();
        cookie.toHeaderCookie(headers);
        this.body = body;
    }


    public PublishVideoBean getPojo() throws HttpException {
        String page = null;
        try {
            page = getPage();
        } catch (Exception e) {
            throw new HttpException("访问URL失败", e);
        }
        if (page == null) {
            return null;
        }
        PublishVideoBean bean = JSONObject.parseObject(page, PublishVideoBean.class);
        return bean;
    }

    /**
     * 获取预上传的信息
     *
     * @return
     * @throws IOException
     * @throws NoSuchAlgorithmException
     * @throws KeyStoreException
     * @throws URISyntaxException
     * @throws KeyManagementException
     */
    public String getPage() throws IOException, NoSuchAlgorithmException, KeyStoreException, URISyntaxException, KeyManagementException {
        HttpClientResult result = HttpClientUtils.doPost(URL+"?csrf="+cookie.getCsrf(), headers, null,new StringEntity(body));
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
