package top.sshh.bililiverecoder.util.bili.upload;

import com.alibaba.fastjson.JSONObject;
import org.apache.http.HttpException;
import top.sshh.bililiverecoder.util.bili.Cookie;
import top.sshh.bililiverecoder.util.bili.HttpClientResult;
import top.sshh.bililiverecoder.util.bili.HttpClientUtils;
import top.sshh.bililiverecoder.util.bili.upload.pojo.EditorPreUploadBean;

import java.io.IOException;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

/**
 * @author mwxmmy
 */
public class EditorPreUploadRequest {

    private static final String URL = "https://api.bilibili.com/studio/video-editor-interface/video-editor/resource";
    private Cookie cookie;
    private HashMap<String, String> headers = new HashMap<String, String>();
    private Map<String, String> params;

    public EditorPreUploadRequest(Cookie cookie, Map<String, String> params) {
        headers.clear();
        this.cookie = cookie;
        cookie.toHeaderCookie(headers);
        this.params = params;
        this.params.put("type", "1");
        this.params.put("csrf", cookie.getCsrf());
    }

    public EditorPreUploadBean getPojo() throws HttpException {
        String page = null;
        try {
            page = getPage();
        } catch (Exception e) {
            throw new HttpException("访问URL失败", e);
        }
        if (page == null) {
            return null;
        }
        EditorPreUploadBean bean = JSONObject.parseObject(page, EditorPreUploadBean.class);
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
        HttpClientResult result = HttpClientUtils.doPost(URL, headers, params);
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
