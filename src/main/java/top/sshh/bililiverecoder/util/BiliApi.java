package top.sshh.bililiverecoder.util;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.jayway.jsonpath.JsonPath;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.util.UriComponents;
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
import java.net.URLEncoder;
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


//    public static String getUserInfo(Long uid) {
//        Map<String, String> additionalHeaders = new HashMap<>();
//        additionalHeaders.put("referer", "https://live.bilibili.com/");
//        additionalHeaders.put("user-agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_14_2) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/71.0.3578.98 Safari/537.36");
//        return HttpClientUtil.get("https://api.bilibili.com/x/space/acc/info?mid=" + uid, additionalHeaders);
//    }

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


    public static String getKeyAndLogin(String username, String password) {
        String loginKeyResp = getLoginKey();
        String hash = JsonPath.read(loginKeyResp, "data.hash");
        String key = JsonPath.read(loginKeyResp, "data.key");
        String loginResp = login(hash, key, username, password,
                "", "", "");
        String codeUrl;
        try {
            codeUrl = JsonPath.read(loginResp, "data.url");
        } catch (Exception e) {
            // 正常
            return loginResp;
        }
        if (StringUtils.isNotBlank(codeUrl)) {
            // 解析url中的challenge
            UriComponents urlComponents = UriComponentsBuilder.fromHttpUrl(codeUrl)
                    .build();
            String challenge = urlComponents.getQueryParams().get("challenge").get(0);
            log.info("请在浏览器中打开 {}", codeUrl);
            log.info("请输入validate :");
            String validate = new Scanner(System.in).nextLine();
            log.info("请输入challenge :");
            challenge = new Scanner(System.in).nextLine();
            String seccode = validate + "|jordan";
            loginKeyResp = getLoginKey();
            hash = JsonPath.read(loginKeyResp, "data.hash");
            key = JsonPath.read(loginKeyResp, "data.key");
            loginResp = login(hash, key, username, password, challenge, seccode, validate);
            System.out.println(loginResp);
        }
        return loginResp;
    }


    public static String login(
            String hash,
            String key,
            String username,
            String password,
            String challenge,
            String seccode,
            String validate) {

        Map<String, String> params = new TreeMap<>();
        params.put("appkey", appKey);
        params.put("build", "101800");
        params.put("channel", "html5_app_bili");
        params.put("mobi_app", "android");
        params.put("platform", "android");
        params.put("ts", "" + System.currentTimeMillis() / 1000);

        Map<String, String> headers = new HashMap<>();
        long currentSecond = Instant.now().getEpochSecond();
        headers.put("Display-ID", "XXD9E43D7A1EBB6669597650E3EE417D9E7F5-" + currentSecond);
        headers.put("Buvid", "XXD9E43D7A1EBB6669597650E3EE417D9E7F5");
        headers.put("User-Agent", "Mozilla/5.0 BiliDroid/5.37.0 (bbcallen@gmail.com)");
        headers.put("Device-ID", "aBRoDWAVeRhsA3FDewMzS3lLMwM");

        params.put("username", username);
        params.put("password", rsa(hash + password, key));
        if (StringUtils.isNotBlank(challenge)) {
            params.put("challenge", challenge);
            params.put("seccode", seccode);
            params.put("validate", validate);
        } else {
            params.put("challenge", "");
            params.put("seccode", "");
            params.put("validate", "");
        }

        params.put("sign", sign(params, appSecret));
        String url = "https://passport.bilibili.com/x/passport-login/oauth2/login";
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


    public static String publish(String accessToken, VideoUploadDto data) {
        String url = "https://member.bilibili.com/x/vu/client/add?access_key=" + accessToken;
        Map<String, String> query = new HashMap<>();
        query.put("access_key", accessToken);
        String sign = sign(query, appSecret);
        url = url + "&sign=" + sign;
        Map<String, String> headers = new HashMap<>();
        long currentSecond = Instant.now().getEpochSecond();
        headers.put("Display-ID", "XXD9E43D7A1EBB6669597650E3EE417D9E7F5-" + currentSecond);
        headers.put("Buvid", "XXD9E43D7A1EBB6669597650E3EE417D9E7F5");
        headers.put("User-Agent", "Mozilla/5.0 BiliDroid/5.37.0 (bbcallen@gmail.com)");
        headers.put("Device-ID", "aBRoDWAVeRhsA3FDewMzS3lLMwM");

        String body = JSON.toJSONString(data);
        return HttpClientUtil.post(url, headers, body);
    }

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
        String url = "https://member.bilibili.com/x/vu/client/cover/up?access_key=" + user.getAccessToken();
        Map<String, String> query = new HashMap<>();
        query.put("access_key", user.getAccessToken());
        String sign = sign(query, appSecret);
        url = url + "&sign=" + sign;
        Map<String, String> headers = new HashMap<>();
        headers.put("User-Agent", "");
        headers.put("cookie", user.getCookies());
        MultipartBody multipartBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("access_key",user.getAccessToken())
                .addFormDataPart("sign",sign(query, appSecret))
                .addFormDataPart("file", fileName, RequestBody.create(fileBytes, MediaType.parse("image/png")))
                .build();
        return HttpClientUtil.post(url, headers, multipartBody);
    }

    public static String uploadChunk(
            String uploadUrl,
            String fileName,
            RandomAccessFile r, long size, int nowChunk,
            int chunkNum) throws IOException {

        long allLength = r.length();
        long start = (nowChunk - 1) * size;
        if(start+size>allLength){
            size = allLength-start;
        }
        ShardingInputStream shardingInputStream = new ShardingInputStream(r, start,size);
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

    public static BiliVideoInfoResponse getVideoInfo(String bvid) {
        String url = "https://api.bilibili.com/x/web-interface/view";
        Map<String, String> params = new TreeMap<>();
        params.put("bvid", bvid);
        Map<String, String> headers = new HashMap<>();
        long currentSecond = Instant.now().getEpochSecond();
        headers.put("Display-ID", "XXD9E43D7A1EBB6669597650E3EE417D9E7F5-" + currentSecond);
        headers.put("Buvid", "XXD9E43D7A1EBB6669597650E3EE417D9E7F5");
        headers.put("User-Agent", "Mozilla/5.0 BiliDroid/5.37.0 (bbcallen@gmail.com)");
        headers.put("Device-ID", "aBRoDWAVeRhsA3FDewMzS3lLMwM");
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
        if(StringUtils.isNotBlank(reply.getParent())){
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
    public static String refreshToken(BiliBiliUser user) {
        String url = "https://passport.bilibili.com/api/v2/oauth2/refresh_token";
        Map<String, String> params = new TreeMap<>();
        params.put("appkey", "4409e2ce8ffd12b8");
        params.put("access_token", user.getAccessToken());
        params.put("refresh_token", user.getRefreshToken());
        params.put("ts", String.valueOf(System.currentTimeMillis()));
        Map<String, String> headers = new HashMap<>();
        if(StringUtils.isNotBlank(user.getCookies())){
            headers.put("cookie", user.getCookies());
        }
        params.put("sign", "" + sign(params, "59b43e04ad6965f34319062b478f83dd"));
        return HttpClientUtil.post(url, headers, params, true);
    }

    public static void main(String[] args) {
        System.out.println(generateQRUrlTV());
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
    }
}
