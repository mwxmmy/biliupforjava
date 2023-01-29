package top.sshh.bililiverecoder.util.bili.upload;

import com.alibaba.fastjson.JSON;
import org.apache.http.HttpException;
import top.sshh.bililiverecoder.util.bili.Cookie;
import top.sshh.bililiverecoder.util.bili.upload.pojo.ChunkUploadBean;
import top.sshh.bililiverecoder.util.bili.upload.pojo.PreUploadBean;

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
public class Main2 {
    public static void main(String[] args) throws HttpException, IOException, NoSuchAlgorithmException, KeyStoreException, URISyntaxException, KeyManagementException {
        Cookie cookie = Cookie.parse("buvid3=048FD76B-11CA-2DA4-C58F-A23268D3DD9056664infoc; i-wanna-go-back=-1; CURRENT_FNVAL=4048; nostalgia_conf=-1; fingerprint=4044dd945a6a72ce062a07d869a4e547; buvid_fp=048FD76B-11CA-2DA4-C58F-A23268D3DD9056664infoc; buvid_fp_plain=undefined; SESSDATA=1c304489%2C1682384802%2C8e2a9%2Aa2; bili_jct=5231419fd14787609dccfed6c6ef262f; DedeUserID=10043269; DedeUserID__ckMd5=7cc21642fb0885c3; sid=4zwb3g33; LIVE_BUVID=AUTO7016668328036494; b_ut=5; rpdid=|(J|~Jk~||Rm0J'uY~uul)mYY; buvid4=null; _uuid=DFAE17C3-E122-57BC-E8E1-B981064B10595901623infoc; CURRENT_QUALITY=16; innersign=0; bp_video_offset_10043269=747614464813039600; b_lsid=7626C89A_1859AAFE491; b_nut=1673337497");

        RandomAccessFile file = new RandomAccessFile("E:/tmp/1.flv", "r");
        long length = file.length();
        Map<String, String> params = new HashMap<>();
        params.put("r", "kodo");
        params.put("profile", "ugcupos/bupfetch");
        params.put("name", "1.flv");
        params.put("size", String.valueOf(length));
        PreUploadRequest request = new PreUploadRequest(cookie, params);
        PreUploadBean pojo = request.getPojo();
        System.out.println(JSON.toJSONString(pojo));
        Map<String, String> chunkParams = new HashMap<>();
        chunkParams.put("size", String.valueOf(length));
        chunkParams.put("start", "0");
        chunkParams.put("end", String.valueOf(length));
        KodoChunkUploadRequest chunkUploadRequest = new KodoChunkUploadRequest(pojo, chunkParams, file);
        ChunkUploadBean chunkUploadBean = chunkUploadRequest.getPojo();
        System.out.println(JSON.toJSONString(chunkUploadBean));

        params.clear();
        params.put("total", String.valueOf(length));
        KodoCompleteUploadRequest completeUploadRequest = new KodoCompleteUploadRequest(pojo, params, chunkUploadBean.getCtx());
        String page1 = completeUploadRequest.getPage();
        System.out.println(page1);
        KodoFetchUploadRequest checkUploadRequest = new KodoFetchUploadRequest(pojo);
        String page = checkUploadRequest.getPage();
        System.out.println(page);


//        String body = "{\"cover\":\"https://archive.biliimg.com/bfs/archive/2320ee63a428d8ee66a4abfe885569b1e4f66248.jpg\",\"title\":\"test1\",\"copyright\":1,\"tid\":21,\"tag\":\"自用\",\"desc_format_id\":0,\"desc\":\"1\",\"recreate\":-1,\"dynamic\":\"\",\"interactive\":0,\"videos\":[{\"filename\":\"" + pojo.getBili_filename() + "\",\"title\":\"test1\",\"desc\":\"\"}],\"act_reserve_create\":0,\"no_disturbance\":0,\"no_reprint\":1,\"open_elec\":1,\"subtitle\":{\"open\":0,\"lan\":\"\"},\"dolby\":0,\"lossless_music\":0,\"up_selection_reply\":false,\"up_close_reply\":false,\"up_close_danmu\":false,\"web_os\":1,\"csrf\":\"" + cookie.getCsrf() + "\"}";
//        PublishVideoRequest publishVideoRequest = new PublishVideoRequest(cookie, body);
//        String page2 = publishVideoRequest.getPage();
//        System.out.println(page2);
    }
}
