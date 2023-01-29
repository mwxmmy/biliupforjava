package top.sshh.bililiverecoder.util.bili.upload;

import com.alibaba.fastjson.JSON;
import org.apache.http.HttpException;
import top.sshh.bililiverecoder.util.bili.Cookie;
import top.sshh.bililiverecoder.util.bili.upload.pojo.LineUploadBean;
import top.sshh.bililiverecoder.util.bili.upload.pojo.PreUploadBean;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author mwxmmy
 */
public class Main {
    public static void main(String[]args) throws HttpException, IOException, NoSuchAlgorithmException, KeyStoreException, URISyntaxException, KeyManagementException {
        Cookie cookie = Cookie.parse("bili_jct:7749e4f893e5f01ded245feb50076f1e; DedeUserID:3493090360821990; DedeUserID__ckMd5:0e2062d25a92649c; sid:p32xoz15; SESSDATA:7b7c8436%2C1688624751%2C20c7d911; ");

        RandomAccessFile file = new RandomAccessFile("E:/tmp/1.flv", "r");
        Map<String, String> params = new HashMap<>();
        params.put("r","upos");
        params.put("profile","ugcupos/bup");
        params.put("name","1.flv");
        params.put("size", String.valueOf(file.length()));
        PreUploadRequest request = new PreUploadRequest(cookie,params);
        request.setLineQuery("?os=upos&zone=sz&upcdn=bda2");
        PreUploadBean pojo = request.getPojo();
        System.out.println(JSON.toJSONString(pojo));
        LineUploadRequest uploadRequest = new LineUploadRequest(cookie,pojo);
        LineUploadBean uploadBean = uploadRequest.getPojo();
        System.out.println(JSON.toJSONString(uploadBean));
        Map<String, String> chunkParams = new HashMap<>();
        chunkParams.put("partNumber","1");
        chunkParams.put("uploadId",uploadBean.getUpload_id());
        chunkParams.put("name", "1.flv");
        chunkParams.put("chunk", "0");
        chunkParams.put("chunks", "1");
        chunkParams.put("size", String.valueOf(file.length()));
        chunkParams.put("start", "0");
        chunkParams.put("end", String.valueOf(file.length()));
        chunkParams.put("total", String.valueOf(file.length()));
        ChunkUploadRequest chunkUploadRequest = new ChunkUploadRequest(pojo, chunkParams, file);
        String page = chunkUploadRequest.getPage();
        System.out.println(page);
        params.clear();
        params.put("uploadId", uploadBean.getUpload_id());
        params.put("output", "json");
        params.put("biz_id", String.valueOf(pojo.getBiz_id()));
        params.put("profile", "ugcupos/bup");
        params.put("name", "1.flv");
        Map<String, Object> bodyMap = new LinkedHashMap<>(1);
        Map<String, Object> partMap = new LinkedHashMap<>(2);
        partMap.put("partNumber", 1);
        partMap.put("eTag", "etag");
        bodyMap.put("parts", new Object[]{partMap});
        CompleteUploadRequest checkUploadRequest = new CompleteUploadRequest(pojo, params, JSON.toJSONString(bodyMap));
        String page1 = checkUploadRequest.getPage();
        System.out.println(page1);
        String body = "{\"cover\":\"https://archive.biliimg.com/bfs/archive/2320ee63a428d8ee66a4abfe885569b1e4f66248.jpg\",\"title\":\"test1\",\"copyright\":1,\"tid\":21,\"tag\":\"自用\",\"desc_format_id\":0,\"desc\":\"1\",\"recreate\":-1,\"dynamic\":\"\",\"interactive\":0,\"videos\":[{\"filename\":\"" + uploadBean.getFileName() + "\",\"title\":\"test1\",\"desc\":\"\"}],\"act_reserve_create\":0,\"no_disturbance\":0,\"no_reprint\":1,\"open_elec\":1,\"subtitle\":{\"open\":0,\"lan\":\"\"},\"dolby\":0,\"lossless_music\":0,\"up_selection_reply\":false,\"up_close_reply\":false,\"up_close_danmu\":false,\"web_os\":1,\"csrf\":\"" + cookie.getCsrf() + "\"}";
        PublishVideoRequest publishVideoRequest = new PublishVideoRequest(cookie, body);
        // String page2 = publishVideoRequest.getPage();
        // System.out.println(page2);
    }
}
