package top.sshh.bililiverecoder.util;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import lombok.Data;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Headers;
import okhttp3.Request;
import okhttp3.Response;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.util.UriComponentsBuilder;
import top.sshh.bililiverecoder.entity.BiliBiliUser;
import top.sshh.bililiverecoder.entity.LiveMsg;
import top.sshh.bililiverecoder.entity.data.*;
import top.sshh.bililiverecoder.util.bili.Cookie;
import top.sshh.bililiverecoder.util.bili.WebCookie;

import javax.crypto.Cipher;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
public class BiliApi {


    // TODO 修改为从properties中读取
    private static String appKey = "4409e2ce8ffd12b8";
    private static String appSecret = "59b43e04ad6965f34319062b478f83dd";


    public static String getLoginKey() {
        String url = "https://passport.bilibili.com/api/oauth2/getKey";
        Map<String, String> params = new TreeMap<>();
        params.put("appkey", appKey);
        params.put("build", "101800");
        params.put("channel", "html5_app_bili");
        params.put("mobi_app", "android");
        params.put("platform", "android");
        params.put("ts", "" + System.currentTimeMillis() / 1000);
        params.put("sign", sign(params, appSecret));
        Map<String, String> headers = new HashMap<>();
        long currentSecond = Instant.now().getEpochSecond();
        headers.put("Display-ID", "XXD9E43D7A1EBB6669597650E3EE417D9E7F5-" + currentSecond);
        headers.put("Buvid", "XXD9E43D7A1EBB6669597650E3EE417D9E7F5");
        headers.put("User-Agent", "Mozilla/5.0 BiliDroid/5.37.0 (bbcallen@gmail.com)");
        headers.put("Device-ID", "aBRoDWAVeRhsA3FDewMzS3lLMwM");
        UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromHttpUrl(url);
        params.forEach(uriBuilder::queryParam);
        return HttpClientUtil.post(url, headers, params, true);
    }


    public static String sign(Map<String, String> params, String appSecret) {
        // 签名规则： md5(url编码后的请求参数（body）)
        String body = params.entrySet().stream()
                .map(e -> {
                    try {
                        return e.getKey() + "=" + URLEncoder.encode(e.getValue(), String.valueOf(StandardCharsets.UTF_8));
                    } catch (UnsupportedEncodingException ex) {
                        ex.printStackTrace();
                        return null;
                    }
                })
                .collect(Collectors.joining("&"));
        return DigestUtils.md5Hex(body + appSecret);
    }

    public static String rsa(String str, String key) {
        try {
            key = key
                    .replace("-----BEGIN PUBLIC KEY-----", "")
                    .replaceAll("\n", "")
                    .replace("-----END PUBLIC KEY-----", "");
            byte[] decode = Base64.getDecoder().decode(key);
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(decode);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            PublicKey publicKey = keyFactory.generatePublic(keySpec);
            Cipher encryptCipher = Cipher.getInstance("RSA");
            encryptCipher.init(Cipher.ENCRYPT_MODE, publicKey);
            byte[] secretMessageBytes = str.getBytes(StandardCharsets.UTF_8);
            byte[] encryptedMessageBytes = encryptCipher.doFinal(secretMessageBytes);
            return Base64.getEncoder().encodeToString(encryptedMessageBytes);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage());
        }
    }

    public static String preUpload(BiliBiliUser user, String profile) {
        String url = "https://member.bilibili.com/preupload";
        Map<String, String> params = new TreeMap<>();
        params.put("appkey", appKey);
        params.put("access_key", user.getAccessToken());
        params.put("build", "2100400");
        params.put("channel", "html5_app_bili");
        params.put("mobi_app", "android");
        params.put("platform", "android");
        params.put("ts", "" + System.currentTimeMillis() / 1000);
        params.put("sign", sign(params, appSecret));

        params.put("profile", profile);
        params.put("mid", user.getUid().toString());

        Map<String, String> headers = new HashMap<>();
        long currentSecond = Instant.now().getEpochSecond();
        headers.put("Display-ID", "XXD9E43D7A1EBB6669597650E3EE417D9E7F5-" + currentSecond);
        headers.put("Buvid", "XXD9E43D7A1EBB6669597650E3EE417D9E7F5");
        headers.put("User-Agent", "Mozilla/5.0 BiliDroid/5.37.0 (bbcallen@gmail.com)");
        headers.put("Device-ID", "aBRoDWAVeRhsA3FDewMzS3lLMwM");
        UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromHttpUrl(url);
        params.forEach(uriBuilder::queryParam);
        return HttpClientUtil.get(uriBuilder.toUriString(), headers);
    }

    public static String preUpload(BiliBiliUser user, Map<String, String> param) {
        String url = "https://member.bilibili.com/preupload";
        Map<String, String> params = new TreeMap<>();
        // params.put("appkey", appKey);
        // params.put("access_key", user.getAccessToken());
        params.put("build", "2100400");
        params.put("channel", "html5_app_bili");
        params.put("mobi_app", "android");
        params.put("platform", "android");
        params.put("ts", "" + System.currentTimeMillis() / 1000);
        // params.put("sign", sign(params, appSecret));
        params.putAll(param);

        Map<String, String> headers = new HashMap<>();
        long currentSecond = Instant.now().getEpochSecond();
        headers.put("Display-ID", "XXD9E43D7A1EBB6669597650E3EE417D9E7F5-" + currentSecond);
        headers.put("Buvid", "XXD9E43D7A1EBB6669597650E3EE417D9E7F5");
        headers.put("User-Agent", "Mozilla/5.0 BiliDroid/5.37.0 (bbcallen@gmail.com)");
        headers.put("Device-ID", "aBRoDWAVeRhsA3FDewMzS3lLMwM");

        headers.put("cookie", user.getCookies());
        UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromHttpUrl(url);
        params.forEach(uriBuilder::queryParam);
        return HttpClientUtil.get(uriBuilder.toUriString(), headers);
    }


    public static String webPublish(BiliBiliUser user, VideoUploadDto data) {
        WebCookie cookie = Cookie.parse(user.getCookies());
        String url = "https://member.bilibili.com/x/vu/web/add/v3?t=" + System.currentTimeMillis() + "&csrf=" + cookie.getCsrf();
        Map<String, String> headers = new HashMap<>();
        data.setCsrf(cookie.getCsrf());
        BiliResponseDto<BillBuvId> buvId = getBuvId();
        headers.put("cookie", cookie.getCookie() + "buvid3=" + buvId.getData().getB3() + ";buvid4=" + buvId.getData().getB4());
        String body = JSON.toJSONString(data);
        return HttpClientUtil.post(url, headers, body);
    }


    // public static String clientPublish(String accessToken, VideoUploadDto data) {
    //     String url = "https://member.bilibili.com/x/vu/client/add?access_key=" + accessToken;
    //     Map<String, String> query = new HashMap<>();
    //     query.put("access_key", accessToken);
    //     String sign = sign(query, appSecret);
    //     url = url + "&sign=" + sign;
    //     Map<String, String> headers = new HashMap<>();
    //     long currentSecond = Instant.now().getEpochSecond();
    //     headers.put("Display-ID", "XXD9E43D7A1EBB6669597650E3EE417D9E7F5-" + currentSecond);
    //     headers.put("Buvid", "XXD9E43D7A1EBB6669597650E3EE417D9E7F5");
    //     headers.put("User-Agent", "Mozilla/5.0 BiliDroid/5.37.0 (bbcallen@gmail.com)");
    //     headers.put("Device-ID", "aBRoDWAVeRhsA3FDewMzS3lLMwM");
    //
    //     String body = JSON.toJSONString(data);
    //     return HttpClientUtil.post(url, headers, body);
    // }

    public static String editPublish(BiliBiliUser user, VideoEditUploadDto data) {
        WebCookie cookie = Cookie.parse(user.getCookies());
        String url = "https://member.bilibili.com/x/vu/web/edit?t=" + System.currentTimeMillis() + "&csrf=" + cookie.getCsrf();
        Map<String, String> headers = new HashMap<>();
        long currentSecond = Instant.now().getEpochSecond();
        headers.put("Display-ID", "XXD9E43D7A1EBB6669597650E3EE417D9E7F5-" + currentSecond);
        headers.put("Buvid", "XXD9E43D7A1EBB6669597650E3EE417D9E7F5");
        headers.put("User-Agent", "Mozilla/5.0 BiliDroid/5.37.0 (bbcallen@gmail.com)");
        headers.put("Device-ID", "aBRoDWAVeRhsA3FDewMzS3lLMwM");
        data.setCsrf(cookie.getCsrf());
        headers.put("cookie", cookie.getCookie());
        String body = JSON.toJSONString(data);
        return HttpClientUtil.post(url, headers, body);
    }

    public static String uploadCover(BiliBiliUser user, String fileName, byte[] fileBytes) {
        String url = "https://member.bilibili.com/x/vu/web/cover/up";
        WebCookie cookie = Cookie.parse(user.getCookies());
        Map<String, String> query = new HashMap<>();
        query.put("t", String.valueOf(System.currentTimeMillis()));
        query.put("cover","data:image/png;base64," + new String(org.apache.commons.codec.binary.Base64.encodeBase64(fileBytes)));
        Map<String, String> headers = new HashMap<>();
        headers.put("User-Agent", "");
        headers.put("Content-Type", "application/x-www-form-urlencoded");
        headers.put("cookie", cookie.getCookie());
        return HttpClientUtil.post(url, headers,"");
    }

    public static String uploadChunk(
            String uploadUrl,
            String fileName,
            RandomAccessFile r, long size, int nowChunk,
            int chunkNum) throws IOException {

        long allLength = r.length();
        long start = (nowChunk - 1) * size;
        if (start + size > allLength) {
            size = allLength - start;
        }
        ShardingInputStream shardingInputStream = new ShardingInputStream(r, start, size);
        String md5 = DigestUtils.md5Hex(shardingInputStream);
        shardingInputStream.reset();
        ChunkUploadRequestBody chunkUploadRequestBody = new ChunkUploadRequestBody(shardingInputStream);
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("version", "2.0.0.1054");
        params.put("filesize", "" + size);
        params.put("chunk", "" + nowChunk);
        params.put("chunks", "" + chunkNum);
        params.put("md5", md5);
        params.put("file", chunkUploadRequestBody);
        Map<String, String> headers = new HashMap<>();
        headers.put("Cookie", "PHPSESSID=" + fileName);
        return HttpClientUtil.upload(uploadUrl, headers, params);
    }

    public static String completeUpload(String url, Integer chunks,
                                        Long filesize,
                                        String md5,
                                        String name,
                                        String version) {
        Map<String, String> params = new HashMap<>();
        params.put("chunks", "" + chunks);
        params.put("filesize", "" + filesize);
        params.put("md5", "" + md5);
        params.put("name", "" + name);
        params.put("version", "" + version);
        return HttpClientUtil.post(url, new HashMap<>(), params, true);

    }

    public static String appMyInfo(BiliBiliUser user) {
        String url = "https://api.bilibili.com/x/member/web/account";
        Map<String, String> params = new TreeMap<>();
        params.put("appkey", appKey);
        params.put("access_key", user.getAccessToken());
        params.put("build", "101800");
        params.put("channel", "html5_app_bili");
        params.put("mobi_app", "android");
        params.put("platform", "android");
        params.put("ts", "" + System.currentTimeMillis() / 1000);
        params.put("sign", sign(params, appSecret));
        Map<String, String> headers = new HashMap<>();
        long currentSecond = Instant.now().getEpochSecond();
        headers.put("Display-ID", "XXD9E43D7A1EBB6669597650E3EE417D9E7F5-" + currentSecond);
        headers.put("Buvid", "XXD9E43D7A1EBB6669597650E3EE417D9E7F5");
        headers.put("User-Agent", "Mozilla/5.0 BiliDroid/5.37.0 (bbcallen@gmail.com)");
        headers.put("Device-ID", "aBRoDWAVeRhsA3FDewMzS3lLMwM");
        headers.put("cookie", user.getCookies());
        headers.put("x-bili-aurora-eid", "UlMFQVcABlAH");
        headers.put("x-bili-aurora-zone", "sh001");
        headers.put("app-key", "android64");
        UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromHttpUrl(url);
        params.forEach(uriBuilder::queryParam);
        return HttpClientUtil.get(uriBuilder.toUriString(), headers);
    }

    /**
     * 加入合集
     *
     * @param user
     * @return
     */
    public static String addSeasons(BiliBiliUser user, long sectionId, String aid, String cid, String title) {
        WebCookie cookie = Cookie.parse(user.getCookies());
        String url = "https://member.bilibili.com/x2/creative/web/season/section/episodes/add?t=" + System.currentTimeMillis() + "&csrf=" + cookie.getCsrf();
        Map<String, Object> params = new TreeMap<>();
        params.put("csrf", cookie.getCsrf());
        params.put("sectionId", sectionId);
        Map<String, Object> episodes = new HashMap<>();
        episodes.put("aid", Long.valueOf(aid));
        episodes.put("cid", Long.valueOf(cid));
        episodes.put("title", title);
        episodes.put("charging_pay", 0);
        params.put("episodes", Collections.singletonList(episodes));
        Map<String, String> headers = new HashMap<>();
        headers.put("referer", "https://member.bilibili.com/platform/upload/video/frame?page_from=creative_home_top_upload");
        headers.put("user-agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_14_2) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/71.0.3578.98 Safari/537.36");
        BiliResponseDto<BillBuvId> buvId = getBuvId();
        headers.put("cookie", cookie.getCookie() + "buvid3=" + buvId.getData().getB3() + ";buvid4=" + buvId.getData().getB4());
        return HttpClientUtil.postJson(url, headers, params, true);
    }

    public static String getSeasons(BiliBiliUser user) {
        WebCookie cookie = Cookie.parse(user.getCookies());
        String url = "https://member.bilibili.com/x2/creative/web/seasons?pn=1&ps=50";
        Map<String, String> headers = new HashMap<>();
        headers.put("referer", "https://member.bilibili.com/platform/upload/video/frame?page_from=creative_home_top_upload");
        headers.put("user-agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_14_2) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/71.0.3578.98 Safari/537.36");
        BiliResponseDto<BillBuvId> buvId = getBuvId();
        headers.put("cookie", cookie.getCookie() + "buvid3=" + buvId.getData().getB3() + ";buvid4=" + buvId.getData().getB4());
        return HttpClientUtil.get(url, headers);
    }

    public static BiliVideoInfoResponse getVideoInfo(BiliBiliUser user,String bvid) {
        String url = "https://api.bilibili.com/x/web-interface/view";
        Map<String, String> params = new TreeMap<>();
        params.put("bvid", bvid);
        Map<String, String> headers = new HashMap<>();
        long currentSecond = Instant.now().getEpochSecond();
        headers.put("Display-ID", "XXD9E43D7A1EBB6669597650E3EE417D9E7F5-" + currentSecond);
        headers.put("Buvid", "XXD9E43D7A1EBB6669597650E3EE417D9E7F5");
        headers.put("User-Agent", "Mozilla/5.0 BiliDroid/5.37.0 (bbcallen@gmail.com)");
        headers.put("Device-ID", "aBRoDWAVeRhsA3FDewMzS3lLMwM");
        if(user != null){
            WebCookie cookie = Cookie.parse(user.getCookies());
            BiliResponseDto<BillBuvId> buvId = getBuvId();
            headers.put("cookie", cookie.getCookie() + "buvid3=" + buvId.getData().getB3() + ";buvid4=" + buvId.getData().getB4());
        }
        UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromHttpUrl(url);
        params.forEach(uriBuilder::queryParam);
        String response = HttpClientUtil.get(uriBuilder.toUriString(), headers);
        return JSON.parseObject(response, BiliVideoInfoResponse.class);
    }

    public static BiliVideoPartInfoResponse getVideoPartInfo(BiliBiliUser user, String bvid) {
        String url = "https://member.bilibili.com/x/vupre/web/archive/view";
        Map<String, String> params = new TreeMap<>();
        params.put("topic_grey", "1");
        params.put("bvid", bvid);
        params.put("t", String.valueOf(System.currentTimeMillis()));
        Map<String, String> headers = new HashMap<>();
        long currentSecond = Instant.now().getEpochSecond();
        headers.put("Display-ID", "XXD9E43D7A1EBB6669597650E3EE417D9E7F5-" + currentSecond);
        headers.put("Buvid", "XXD9E43D7A1EBB6669597650E3EE417D9E7F5");
        headers.put("User-Agent", "Mozilla/5.0 BiliDroid/5.37.0 (bbcallen@gmail.com)");
        headers.put("Device-ID", "aBRoDWAVeRhsA3FDewMzS3lLMwM");
        WebCookie cookie = Cookie.parse(user.getCookies());
        headers.put("cookie", cookie.getCookie());
        UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromHttpUrl(url);
        params.forEach(uriBuilder::queryParam);
        String response = HttpClientUtil.get(uriBuilder.toUriString(), headers);
        return JSON.parseObject(response, BiliVideoPartInfoResponse.class);
    }

    public static BiliDmResponse sendVideoDm(BiliBiliUser user, LiveMsg msg) {
        String url = "https://api.bilibili.com/x/v2/dm/post";
        Map<String, String> params = new TreeMap<>();
        params.put("appkey", appKey);
        params.put("access_key", user.getAccessToken());
        params.put("build", "105301");
        params.put("channel", "html5_app_bili");
        params.put("mobi_app", "android");
        params.put("platform", "android");
        params.put("ts", "" + System.currentTimeMillis() / 1000);
        params.put("sign", sign(params, appSecret));
        params.put("type", "1");
        params.put("pool", String.valueOf(msg.getPool()));
        params.put("oid", msg.getCid().toString());
        params.put("bvid", msg.getBvid());
        params.put("msg", msg.getContext());
        params.put("color", String.valueOf(msg.getColor()));
        params.put("fontsize", String.valueOf(msg.getFontsize()));
        params.put("progress", msg.getSendTime().toString());
        params.put("mode", String.valueOf(msg.getMode()));
        params.put("rnd", String.valueOf(System.currentTimeMillis() * 1000000));
        Map<String, String> headers = new HashMap<>();
        long currentSecond = Instant.now().getEpochSecond();
        headers.put("Display-ID", "XXD9E43D7A1EBB6669597650E3EE417D9E7F5-" + currentSecond);
        headers.put("Buvid", "XXD9E43D7A1EBB6669597650E3EE417D9E7F5");
        headers.put("User-Agent", "Mozilla/5.0 BiliDroid/5.37.0 (bbcallen@gmail.com)");
        headers.put("Device-ID", "aBRoDWAVeRhsA3FDewMzS3lLMwM");
        headers.put("cookie", user.getCookies());
        headers.put("x-bili-aurora-eid", "UlMFQVcABlAH");
        headers.put("x-bili-aurora-zone", "sh001");
        headers.put("app-key", "android64");
        String response = HttpClientUtil.post(url, headers, params, true);
        return JSON.parseObject(response, BiliDmResponse.class);
    }

    public static BiliReplyResponse sendVideoReply(BiliBiliUser user, BiliReply reply) {
        String url = "https://api.bilibili.com/x/v2/reply/add";
        Map<String, String> params = new TreeMap<>();
        params.put("appkey", appKey);
        params.put("access_key", user.getAccessToken());
        params.put("build", "105301");
        params.put("channel", "html5_app_bili");
        params.put("mobi_app", "android");
        params.put("platform", "android");
        params.put("ts", "" + System.currentTimeMillis() / 1000);
        params.put("sign", sign(params, appSecret));
        params.put("type", reply.getType());
        params.put("message", reply.getMessage());
        params.put("oid", reply.getOid());
        if (StringUtils.isNotBlank(reply.getParent())) {
            params.put("root", reply.getRoot());
            params.put("parent", reply.getParent());
        }
        params.put("plat", "2");
        Map<String, String> headers = new HashMap<>();
        long currentSecond = Instant.now().getEpochSecond();
        headers.put("Display-ID", "XXD9E43D7A1EBB6669597650E3EE417D9E7F5-" + currentSecond);
        headers.put("Buvid", "XXD9E43D7A1EBB6669597650E3EE417D9E7F5");
        headers.put("User-Agent", "Mozilla/5.0 BiliDroid/5.37.0 (bbcallen@gmail.com)");
        headers.put("Device-ID", "aBRoDWAVeRhsA3FDewMzS3lLMwM");
        headers.put("cookie", user.getCookies());
        headers.put("x-bili-aurora-eid", "UlMFQVcABlAH");
        headers.put("x-bili-aurora-zone", "sh001");
        headers.put("app-key", "android64");
        String response = HttpClientUtil.post(url, headers, params, true);
        return JSON.parseObject(response, BiliReplyResponse.class);
    }

    public static BiliReplyResponse topVideoReply(BiliBiliUser user, BiliReply reply) {
        String url = "https://api.bilibili.com/x/v2/reply/top";
        Map<String, String> params = new TreeMap<>();
        params.put("appkey", appKey);
        params.put("access_key", user.getAccessToken());
        params.put("build", "105301");
        params.put("channel", "html5_app_bili");
        params.put("mobi_app", "android");
        params.put("platform", "android");
        params.put("ts", "" + System.currentTimeMillis() / 1000);
        params.put("sign", sign(params, appSecret));
        params.put("type", reply.getType());
        params.put("oid", reply.getOid());
        params.put("rpid", reply.getRpid());
        params.put("action", reply.getAction());
        Map<String, String> headers = new HashMap<>();
        long currentSecond = Instant.now().getEpochSecond();
        headers.put("Display-ID", "XXD9E43D7A1EBB6669597650E3EE417D9E7F5-" + currentSecond);
        headers.put("Buvid", "XXD9E43D7A1EBB6669597650E3EE417D9E7F5");
        headers.put("User-Agent", "Mozilla/5.0 BiliDroid/5.37.0 (bbcallen@gmail.com)");
        headers.put("Device-ID", "aBRoDWAVeRhsA3FDewMzS3lLMwM");
        headers.put("cookie", user.getCookies());
        headers.put("x-bili-aurora-eid", "UlMFQVcABlAH");
        headers.put("x-bili-aurora-zone", "sh001");
        headers.put("app-key", "android64");
        String response = HttpClientUtil.post(url, headers, params, true);
        return JSON.parseObject(response, BiliReplyResponse.class);
    }


    public static BiliResponseDto<GenerateQRDto> generateQRUrlTV() {
        Map<String, String> params = new TreeMap<>();
        params.put("appkey", "4409e2ce8ffd12b8");
        params.put("local_id", "0");
        params.put("ts", "0");
        params.put("sign", "" + sign(params, "59b43e04ad6965f34319062b478f83dd"));
        String res = HttpClientUtil.post("https://passport.bilibili.com/x/passport-tv-login/qrcode/auth_code", new HashMap<>(), params, true);
        BiliResponseDto<GenerateQRDto> resp = JSON.parseObject(res, new TypeReference<BiliResponseDto<GenerateQRDto>>() {
        });
        return resp;
    }

    public static BiliResponseDto<GenerateQRDto> generateQRUrlWeb() {
        Map<String, String> headers = new TreeMap<>();
        String res = HttpClientUtil.get("https://passport.bilibili.com/x/passport-login/web/qrcode/generate", headers);
        BiliResponseDto<GenerateQRDto> resp = JSON.parseObject(res, new TypeReference<BiliResponseDto<GenerateQRDto>>() {
        });
        return resp;
    }


    public static String loginOnTV(String authCode) {
        String url = "https://passport.bilibili.com/x/passport-tv-login/qrcode/poll";
        Map<String, String> params = new TreeMap<>();
        params.put("appkey", "4409e2ce8ffd12b8");
        params.put("auth_code", authCode);
        params.put("local_id", "0");
        params.put("ts", "0");
        params.put("sign", "" + sign(params, "59b43e04ad6965f34319062b478f83dd"));
        return HttpClientUtil.post(url, new HashMap<>(), params, true);
    }

    public static BiliWebLoginDto loginOnWeb(String qrcodeKey) {
        String url = "https://passport.bilibili.com/x/passport-login/web/qrcode/poll?qrcode_key=" + qrcodeKey;
        Map<String, String> headers = new TreeMap<>();
        try {
            Response response = HttpClientUtil.getClient().newCall(new Request.Builder()
                    .url(url)
                    .headers(Headers.of(headers))
                    .get()
                    .build()
            ).execute();
            String loginResp = response.body().string();
            BiliWebLoginDto webLoginDto = JSON.parseObject(loginResp, BiliWebLoginDto.class);
            if (webLoginDto.getCode() == 0 && StringUtils.isNotBlank(webLoginDto.getData().getUrl())) {
                String url2 = webLoginDto.getData().getUrl();
                String SESSDATA = getParameterValueFromUrl(url2, "SESSDATA");
                String bili_jct = getParameterValueFromUrl(url2, "bili_jct");
                String DedeUserID = getParameterValueFromUrl(url2, "DedeUserID");
                String DedeUserID__ckMd5 = getParameterValueFromUrl(url2, "DedeUserID__ckMd5");
                String sid = getParameterValueFromUrl(url2, "sid");
                webLoginDto.setCookie("bili_jct=" + bili_jct + ";SESSDATA=" + SESSDATA + ";DedeUserID=" + DedeUserID + ";DedeUserID__ckMd5=" + DedeUserID__ckMd5 + ";sid+" + sid + ";");
            }
            return webLoginDto;
        } catch (
                UnknownHostException e) {
            try {
                Thread.sleep(5000L);
            } catch (Exception ignored) {
            }

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage());
        }
        return null;
    }

    public static BiliResponseDto<BillBuvId> getBuvId() {
        String url = "https://api.bilibili.com/x/frontend/finger/spi";
        Map<String, String> additionalHeaders = new HashMap<>();
        additionalHeaders.put("referer", "https://live.bilibili.com/");
        additionalHeaders.put("user-agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_14_2) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/71.0.3578.98 Safari/537.36");
        String res = HttpClientUtil.get(url, additionalHeaders);
        BiliResponseDto<BillBuvId> resp = JSON.parseObject(res, new TypeReference<BiliResponseDto<BillBuvId>>() {
        });
        return resp;
    }

    public static String refreshToken(BiliBiliUser user) {
        String url = "https://passport.bilibili.com/api/v2/oauth2/refresh_token";
        Map<String, String> params = new TreeMap<>();
        params.put("appkey", "4409e2ce8ffd12b8");
        params.put("access_token", user.getAccessToken());
        params.put("refresh_token", user.getRefreshToken());
        params.put("ts", String.valueOf(System.currentTimeMillis()));
        Map<String, String> headers = new HashMap<>();
        if (StringUtils.isNotBlank(user.getCookies())) {
            headers.put("cookie", user.getCookies());
        }
        params.put("sign", "" + sign(params, "59b43e04ad6965f34319062b478f83dd"));
        return HttpClientUtil.post(url, headers, params, true);
    }

    public static BiliUserCardResponseDto getUserCard(long uid) {
        String url = "https://account.bilibili.com/api/member/getCardByMid?mid=" + uid;
        Map<String, String> additionalHeaders = new HashMap<>();
        additionalHeaders.put("referer", "https://account.bilibili.com/api/member/getCardByMid?mid=" + uid);
        additionalHeaders.put("user-agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_14_2) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/71.0.3578.98 Safari/537.36");
        String res = HttpClientUtil.get(url, additionalHeaders);
        try {
            return JSON.parseObject(res, new TypeReference<BiliUserCardResponseDto>() {
            });
        } catch (Exception e) {
            try {
                url = "https://workers.vrp.moe/api/bilibili/user-info/" + uid;
                res = HttpClientUtil.get(url, additionalHeaders);
                if (res.contains("card")) {
                    return JSON.parseObject(res, new TypeReference<BiliUserCardResponseDto>() {
                    });
                }
            } catch (Exception ignored) {
            }
            BiliUserCardResponseDto cardResponseDto = new BiliUserCardResponseDto();
            cardResponseDto.setCode(-400);
            return cardResponseDto;
        }
    }

    /**
     * 从URL中获取指定名称的查询参数值。
     *
     * @param url           URL字符串
     * @param parameterName 要获取的参数名称
     * @return 指定名称的参数值，如果不存在则返回null
     */
    public static String getParameterValueFromUrl(String url, String parameterName) {
        try {
            // 解析URL
            URI uri = new URI(url);
            String query = uri.getQuery();

            if (query == null || query.isEmpty()) {
                return null;
            }

            // 解析查询参数
            Map<String, String> parameters = new HashMap<>();
            for (String param : query.split("&")) {
                String[] pair = param.split("=");
                if (pair.length == 2) {
                    parameters.put(pair[0], pair[1]);
                } else if (pair.length == 1) {
                    parameters.put(pair[0], "");
                }
            }

            // 返回指定名称的参数值
            return parameters.get(parameterName);

        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Invalid URL: " + url, e);
        }
    }

    public static void main(String[] args) {
        System.out.println(generateQRUrlTV());
    }


    @Data
    @ToString
    public static class BiliUserCardResponseDto {
        // 0：成功 1：参数错误
        private long ts;
        private int code;
        private BiliUserCard card;
    }

    @Data
    public static class BiliResponseDto<T> {
        // 0：成功 1：参数错误
        private Integer code;
        private String msg;
        private String message;
        private T data;
    }

    @Data
    public static class GenerateQRDto {
        private String url;
        private String auth_code;
        private String qrcode_key;
    }
}
