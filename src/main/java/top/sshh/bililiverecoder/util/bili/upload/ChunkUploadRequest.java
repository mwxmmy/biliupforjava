package top.sshh.bililiverecoder.util.bili.upload;

import org.apache.http.entity.InputStreamEntity;
import top.sshh.bililiverecoder.util.ShardingInputStream;
import top.sshh.bililiverecoder.util.bili.HttpClientResult;
import top.sshh.bililiverecoder.util.bili.HttpClientUtils;
import top.sshh.bililiverecoder.util.bili.upload.pojo.PreUploadBean;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

public class ChunkUploadRequest {

    private final String URL;
    private HashMap<String, String> headers = new HashMap<String, String>();
    private PreUploadBean preUploadBean;
    private Map<String, String> params;
    private RandomAccessFile file;


    public ChunkUploadRequest(PreUploadBean preUploadBean, Map<String, String> params, RandomAccessFile file) {
        this.URL = "https:" + preUploadBean.getEndpoint() + preUploadBean.getUpUrl();
        headers.clear();
        headers.put("X-Upos-Auth", preUploadBean.getAuth());
        headers.put("Content-Type", "application/octet-stream");
        this.preUploadBean = preUploadBean;
        this.params = params;
        this.file = file;
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
        ShardingInputStream inputStream = new ShardingInputStream(file, Long.parseLong(params.get("start")), Long.parseLong(params.get("end")));
        InputStreamEntity body = new InputStreamEntity(inputStream, Long.parseLong(params.get("size")));
        HttpClientResult result = HttpClientUtils.doPut(URL, headers, params, body, Integer.parseInt(preUploadBean.getTimeout()) * 1000);
        int code = result.getCode();
        if (code != 200) {
            throw new RuntimeException("上传返回http状态码为：" + code + "，数据为 " + result.getContent());
        }
        return result.getContent();
    }
}