package top.sshh.bililiverecoder.util.bili.upload;

import com.alibaba.fastjson.JSONObject;
import org.apache.http.HttpException;
import org.apache.http.entity.StringEntity;
import top.sshh.bililiverecoder.util.bili.HttpClientResult;
import top.sshh.bililiverecoder.util.bili.HttpClientUtils;
import top.sshh.bililiverecoder.util.bili.upload.pojo.CompleteUploadBean;
import top.sshh.bililiverecoder.util.bili.upload.pojo.PreUploadBean;

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
public class CompleteUploadRequest {

    private final String URL;
    private HashMap<String, String> headers = new HashMap<String, String>();
    private PreUploadBean preUploadBean;
    private String body;

    private Map<String, String> params;

    public CompleteUploadRequest(PreUploadBean preUploadBean, Map<String, String> params, String body) {
        this.URL = "https:" + preUploadBean.getEndpoint() + preUploadBean.getUpUrl() + "?output=json";
        headers.clear();
        headers.put("X-Upos-Auth", preUploadBean.getAuth());
        headers.put("Content-Type", "application/json");
        this.preUploadBean = preUploadBean;
        this.body = body;
        this.params = params;
    }


    public CompleteUploadBean getPojo() throws HttpException {
        String page = null;
        try {
            page = getPage();
        } catch (Exception e) {
            throw new HttpException("访问URL失败", e);
        }
        if (page == null) {
            return null;
        }
        CompleteUploadBean bean = JSONObject.parseObject(page, CompleteUploadBean.class);
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
        HttpClientResult result = HttpClientUtils.doPost(URL, headers, params, new StringEntity(body), 300 * 1000);
        return result.getContent();
    }

}
