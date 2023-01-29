package top.sshh.bililiverecoder.util.bili.upload;

import com.alibaba.fastjson.JSONObject;
import org.apache.http.HttpException;
import top.sshh.bililiverecoder.util.bili.Cookie;
import top.sshh.bililiverecoder.util.bili.HttpClientResult;
import top.sshh.bililiverecoder.util.bili.HttpClientUtils;
import top.sshh.bililiverecoder.util.bili.upload.pojo.EditorSpaceBean;

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
public class EdtiorSpaceRequest {

    private final String URL = "https://api.bilibili.com/studio/video-editor-interface/video-editor/resource/space";

    private Cookie cookie;
    private HashMap<String, String> headers = new HashMap<String, String>();

    private Map<String, String> params = new HashMap<String, String>();

    public EdtiorSpaceRequest(Cookie cookie) {
        this.cookie = cookie;
        cookie.toHeaderCookie(headers);
    }

    public EditorSpaceBean getPojo() throws HttpException {
        String page = null;
        try {
            page = getPage();
        } catch (Exception e) {
            throw new HttpException("访问URL失败", e);
        }
        if (page == null) {
            return null;
        }
        EditorSpaceBean bean = JSONObject.parseObject(page, EditorSpaceBean.class);
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
        HttpClientResult result = HttpClientUtils.doGet(URL, headers, params);
        return result.getContent();
    }

}
