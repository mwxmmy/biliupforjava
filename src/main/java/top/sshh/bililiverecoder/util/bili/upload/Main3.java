package top.sshh.bililiverecoder.util.bili.upload;

import com.alibaba.fastjson.JSON;
import org.apache.http.HttpException;
import top.sshh.bililiverecoder.util.bili.Cookie;
import top.sshh.bililiverecoder.util.bili.upload.pojo.CompleteUploadBean;
import top.sshh.bililiverecoder.util.bili.upload.pojo.EditorPreUploadBean;
import top.sshh.bililiverecoder.util.bili.upload.pojo.EditorSpaceBean;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

/**
 * @author mwxmmy
 */
public class Main3 {
    public static void main(String[] args) throws HttpException, IOException, NoSuchAlgorithmException, KeyStoreException, URISyntaxException, KeyManagementException {
        Cookie cookie = Cookie.parse("bili_jct:2fc6d4818dbc5d9c0c6e697935de2159; DedeUserID:10043269; DedeUserID__ckMd5:7cc21642fb0885c3; sid:7btk4z90; SESSDATA:611bce9f%2C1690002590%2C50606d11; ");

        EdtiorSpaceRequest edtiorSpaceRequest = new EdtiorSpaceRequest(cookie);
        EditorSpaceBean spaceBean = edtiorSpaceRequest.getPojo();
        System.out.println(JSON.toJSONString(spaceBean));


        RandomAccessFile file = new RandomAccessFile("D:/tmp/1.flv", "r");
        long length = file.length();
        Map<String, String> params = new HashMap<>();
        params.put("name", "1.flv");
        params.put("resource_file_type", "flv");
        params.put("size", String.valueOf(length));
        EditorPreUploadRequest request = new EditorPreUploadRequest(cookie, params);
        EditorPreUploadBean pojo = request.getPojo();
        System.out.println(JSON.toJSONString(pojo));
        Map<String, String> chunkParams = new HashMap<>();
        chunkParams.put("index", String.valueOf(0));
        chunkParams.put("start", "0");
        chunkParams.put("end", String.valueOf(length));
        chunkParams.put("size", String.valueOf(length));
        EditorChunkUploadRequest chunkUploadRequest = new EditorChunkUploadRequest(pojo, chunkParams, file);
        String etag = chunkUploadRequest.getPage();
        System.out.println("上传成功");


        params.clear();
        params.put("etags", etag);
        EdtiorCompleteUploadRequest completeUploadRequest = new EdtiorCompleteUploadRequest(cookie, pojo, params);
        CompleteUploadBean completeUploadBean = completeUploadRequest.getPojo();
        System.out.println(JSON.toJSONString(completeUploadBean));
        params.clear();
        EdtiorTranscodeRequest transcodeRequest = new EdtiorTranscodeRequest(cookie, pojo);
        String page = transcodeRequest.getPage();
        System.out.println(page);


        //        String body = "{\"cover\":\"https://archive.biliimg.com/bfs/archive/2320ee63a428d8ee66a4abfe885569b1e4f66248.jpg\",\"title\":\"test1\",\"copyright\":1,\"tid\":21,\"tag\":\"自用\",\"desc_format_id\":0,\"desc\":\"1\",\"recreate\":-1,\"dynamic\":\"\",\"interactive\":0,\"videos\":[{\"filename\":\"" + pojo.getBili_filename() + "\",\"title\":\"test1\",\"desc\":\"\"}],\"act_reserve_create\":0,\"no_disturbance\":0,\"no_reprint\":1,\"open_elec\":1,\"subtitle\":{\"open\":0,\"lan\":\"\"},\"dolby\":0,\"lossless_music\":0,\"up_selection_reply\":false,\"up_close_reply\":false,\"up_close_danmu\":false,\"web_os\":1,\"csrf\":\"" + cookie.getCsrf() + "\"}";
        //        PublishVideoRequest publishVideoRequest = new PublishVideoRequest(cookie, body);
        //        String page2 = publishVideoRequest.getPage();
        //        System.out.println(page2);
    }
}
