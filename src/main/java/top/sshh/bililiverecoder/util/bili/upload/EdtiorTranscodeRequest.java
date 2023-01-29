package top.sshh.bililiverecoder.util.bili.upload;


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
public class EdtiorTranscodeRequest {

    private final String URL = "https://api.bilibili.com/studio/video-editor-interface/video-editor/resource/transcode";

    private Cookie cookie;
    private HashMap<String, String> headers = new HashMap<String, String>();
    private EditorPreUploadBean preUploadBean;

    private Map<String, String> params = new HashMap<>();

    public EdtiorTranscodeRequest(Cookie cookie, EditorPreUploadBean preUploadBean) {
        this.preUploadBean = preUploadBean;
        this.cookie = cookie;
        cookie.toHeaderCookie(headers);
        this.params.put("resource_id", preUploadBean.getData().getResource_id());
        this.params.put("task_type", "1");
        this.params.put("csrf", cookie.getCsrf());
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
        HttpClientResult result = HttpClientUtils.doPost(URL, headers, params, null, 300 * 1000);
        return result.getContent();
    }

}
