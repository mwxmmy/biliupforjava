package top.sshh.bililiverecoder.util.bili.upload;

import com.alibaba.fastjson.JSONObject;
import org.apache.http.HttpException;
import top.sshh.bililiverecoder.util.bili.Cookie;
import top.sshh.bililiverecoder.util.bili.HttpClientResult;
import top.sshh.bililiverecoder.util.bili.HttpClientUtils;
import top.sshh.bililiverecoder.util.bili.upload.pojo.LineUploadBean;
import top.sshh.bililiverecoder.util.bili.upload.pojo.PreUploadBean;

import java.io.IOException;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;

/**
 * @author mwxmmy
 */
public class LineUploadRequest {

    private final String URL;
    private Cookie cookie;
    private HashMap<String, String> headers = new HashMap<String, String>();
    private PreUploadBean preUploadBean;

    public LineUploadRequest(Cookie cookie, PreUploadBean preUploadBean) {
        this.URL = "https:" + preUploadBean.getEndpoint() + preUploadBean.getUpUrl() + "?uploads&output=json";
        this.cookie = cookie;
        headers.clear();
        headers.put("X-Upos-Auth", preUploadBean.getAuth());
        cookie.toHeaderCookie(headers);
        this.preUploadBean = preUploadBean;
    }


    public LineUploadBean getPojo() throws HttpException {
        String page = null;
        try {
            page = getPage();
        } catch (Exception e) {
            throw new HttpException("访问URL失败", e);
        }
        if (page == null) {
            return null;
        }
        LineUploadBean bean = JSONObject.parseObject(page, LineUploadBean.class);
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
        HttpClientResult result = HttpClientUtils.doPost(URL, headers, null);
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
