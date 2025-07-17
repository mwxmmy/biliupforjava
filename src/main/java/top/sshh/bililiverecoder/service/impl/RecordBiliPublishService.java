package top.sshh.bililiverecoder.service.impl;

import com.alibaba.fastjson.JSON;
import com.jayway.jsonpath.JsonPath;
import com.zjiecode.wxpusher.client.WxPusher;
import com.zjiecode.wxpusher.client.bean.Message;
import jakarta.annotation.PostConstruct;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import top.sshh.bililiverecoder.entity.BiliBiliUser;
import top.sshh.bililiverecoder.entity.RecordHistory;
import top.sshh.bililiverecoder.entity.RecordHistoryPart;
import top.sshh.bililiverecoder.entity.RecordRoom;
import top.sshh.bililiverecoder.entity.data.*;
import top.sshh.bililiverecoder.repo.BiliUserRepository;
import top.sshh.bililiverecoder.repo.RecordHistoryPartRepository;
import top.sshh.bililiverecoder.repo.RecordHistoryRepository;
import top.sshh.bililiverecoder.repo.RecordRoomRepository;
import top.sshh.bililiverecoder.service.UploadServiceFactory;
import top.sshh.bililiverecoder.util.BiliApi;
import top.sshh.bililiverecoder.util.TaskUtil;
import top.sshh.bililiverecoder.util.bili.Cookie;
import top.sshh.bililiverecoder.util.bili.WebCookie;
import top.sshh.bililiverecoder.util.bili.user.UserMy;
import top.sshh.bililiverecoder.util.bili.user.UserMyRootBean;

import java.io.File;
import java.io.FileInputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Component
public class RecordBiliPublishService {

    @Value("${record.work-path}")
    private String workPath;

    private static final String WX_MSG_FORMAT = """
            投稿结果: %s
            收到主播%s投稿事件
            房间名: %s
            时间: %s
            原因: %s
            """;

    @Value("${record.wx-push-token}")
    private String wxToken;

    @PostConstruct
    public void initWorkPath() {
        workPath = workPath.replaceAll("\\\\\\\\", "\\\\");
        workPath = workPath.replace("\\", "/");
    }

    @Autowired
    private BiliUserRepository biliUserRepository;
    @Autowired
    private RecordHistoryPartRepository partRepository;
    @Autowired
    private RecordHistoryRepository historyRepository;
    @Autowired
    private RecordRoomRepository roomRepository;
    @Autowired
    private UploadServiceFactory uploadServiceFactory;

    @Async
    public void asyncPublishRecordHistory(RecordHistory history) {
        this.publishRecordHistory(history);
    }

    // 方法用于按"${@数字}"分割字符串
    public static List<String> splitTemplateByUid(String template) {
        List<String> parts = new ArrayList<>();
        Pattern pattern = Pattern.compile("\\$\\{[@]\\d+\\}");
        Matcher matcher = pattern.matcher(template);
        int lastMatchEnd = 0;

        while (matcher.find()) {
            // 如果从上一个匹配结束位置到当前匹配开始之间有内容，则添加这部分内容
            if (lastMatchEnd < matcher.start()) {
                parts.add(template.substring(lastMatchEnd, matcher.start()));
            }

            // 添加"${@数字}"本身
            parts.add(matcher.group());

            // 更新上一个匹配结束位置
            lastMatchEnd = matcher.end();
        }

        // 如果还有剩余的部分，添加到最后
        if (lastMatchEnd < template.length()) {
            parts.add(template.substring(lastMatchEnd));
        }

        return parts;
    }

    @Async
    public void asyncRepublishRecordHistory(RecordHistory history) {

        RecordRoom room = roomRepository.findByRoomId(history.getRoomId());
        String wxuid = room.getWxuid();
        String pushMsgTags = room.getPushMsgTags();
        Optional<BiliBiliUser> userOptional = biliUserRepository.findById(room.getUploadUserId());
        if (!userOptional.isPresent()) {
            log.error("视频发布事件，用户不存在，无法发布 ==>{}", JSON.toJSONString(room));
        }
        BiliBiliUser biliBiliUser = userOptional.get();
        if (!biliBiliUser.isLogin()) {
            log.error("视频发布事件，用户登录状态失效，无法发布，请重新登录 ==>{}", JSON.toJSONString(room));
        }

        // 发布任务入队列
        TaskUtil.publishTask.put(history.getId(), Thread.currentThread());
        StringBuilder errMsg = new StringBuilder("\n");
        try {
            List<RecordHistoryPart> uploadParts = partRepository.findByHistoryIdOrderByStartTimeAsc(history.getId());
            BiliVideoPartInfoResponse videoPartInfo = BiliApi.getVideoPartInfo(biliBiliUser, history.getBvId());
            Map<String, BiliVideoPartInfoResponse.Video> videoMap = videoPartInfo.getData().getVideos().stream().collect(Collectors.toMap(BiliVideoPartInfoResponse.Video::getTitle, Function.identity()));
            for (RecordHistoryPart uploadPart : uploadParts) {
                // 正常分p不需要在重复上传
                BiliVideoPartInfoResponse.Video video = videoMap.get(uploadPart.getTitle());
                // video.getFailCode() == 14 && video.getXcodeState() == 1 时间戳跳变
                if (video == null || (video.getFailCode() == 9 && video.getXcodeState() == 3) || (video.getFailCode() == 14 && video.getXcodeState() == 1) || (video.getFailCode() == 0 && video.getXcodeState() == 2)) {
                    if (video == null) {
                        errMsg.append(uploadPart.getTitle()).append("   视频不存在\n");
                    } else if (video.getXcodeState() == 2) {
                        errMsg.append(uploadPart.getTitle()).append("   转码中\n");
                    } else {
                        errMsg.append(uploadPart.getTitle()).append("   转码失败\n");
                    }
                    uploadPart.setUpload(false);
                    uploadPart.setCid(null);
                    uploadPart.setFileName(null);
                    uploadPart = partRepository.save(uploadPart);
                    String filePath = uploadPart.getFilePath().intern();
                    File file = new File(filePath);
                    if (file.exists()) {
                        synchronized (filePath.intern()) {
                            log.error("视频重新发布流程获取part上传锁成功，即将再次检查是否已上传完成");
                            //再次检查是否上传完成
                            Optional<RecordHistoryPart> partOptional = partRepository.findById(uploadPart.getId());
                            if (partOptional.isPresent()) {
                                RecordHistoryPart part = partOptional.get();
                                if (!part.isUpload()) {
                                    log.error("视频发布流程获取part上传锁成功，检查到未上传完成");
                                    uploadServiceFactory.getUploadService(room.getLine()).upload(uploadPart);
                                }
                            }

                        }
                    }
                }
            }
            uploadParts = partRepository.findByHistoryIdOrderByStartTimeAsc(history.getId());
            userOptional = biliUserRepository.findById(room.getUploadUserId());
            if (!userOptional.isPresent()) {
                log.error("视频发布事件，用户不存在，无法发布 ==>{}", JSON.toJSONString(room));
            }
            biliBiliUser = userOptional.get();
            Map<String, Object> map = new HashMap<>();
            LocalDateTime startTime = history.getStartTime();
            map.put("date", startTime);

            String uname = room.getUname();
            if (uname == null) {
                map.put("${uname}", "");
            } else {
                map.put("${uname}", uname);
            }
            String title = StringUtils.isNotBlank(history.getTitle()) ? history.getTitle() : "直播录像";
            map.put("${title}", title);
            map.put("${roomId}", room.getRoomId());
            map.put("${areaName}", "");
            List<SingleVideoDto> dtos = new ArrayList<>();
            for (int i = 0; i < uploadParts.size(); i++) {
                RecordHistoryPart uploadPart = uploadParts.get(i);
                SingleVideoDto dto = new SingleVideoDto();
                title = StringUtils.isNotBlank(uploadPart.getLiveTitle()) ? uploadPart.getLiveTitle() : "直播录像";
                map.put("${title}", title);
                map.put("date", uploadPart.getStartTime());
                map.put("${index}", Integer.valueOf(i + 1));
                map.put("${areaName}", uploadPart.getAreaName());
                String filePath = uploadPart.getFilePath();
                String fileName = filePath.substring(filePath.lastIndexOf("/") + 1, filePath.lastIndexOf("."));
                map.put("${fileName}", fileName);
                dto.setTitle(this.template(room.getPartTitleTemplate(), map).getDesc());
                //同步标题
                uploadPart.setTitle(this.template(room.getPartTitleTemplate(), map).getDesc());
                uploadPart = partRepository.save(uploadPart);
                dto.setDesc("");
                dto.setFilename(uploadPart.getFileName());
                if (uploadPart.getCid() != null && uploadPart.getCid() > 0) {
                    dto.setCid(uploadPart.getCid());
                }
                dtos.add(dto);
            }
            VideoEditUploadDto videoUploadDto = new VideoEditUploadDto();

            map.put("date", startTime);
            videoUploadDto.setTid(room.getTid());
            videoUploadDto.setCover(history.getCoverUrl());
            videoUploadDto.setCopyright(room.getCopyright());
            videoUploadDto.setTitle(this.template(room.getTitleTemplate(), map).getDesc());
            if (videoUploadDto.getCopyright() == 2) {
                videoUploadDto.setSource(this.template(videoUploadDto.getSource(), map).getDesc());
            }
            videoUploadDto.setDesc(this.template(room.getDescTemplate(), map).getDesc());
            videoUploadDto.setDesc_v2(this.template(room.getDescTemplate(), map).getDescV2Dtos());
            videoUploadDto.setDynamic(this.template(room.getDescTemplate(), map).getDesc());
            videoUploadDto.setDynamic_v2(this.template(room.getDescTemplate(), map).getDescV2Dtos());
            videoUploadDto.setVideos(dtos);
            videoUploadDto.setTag(this.template(room.getTags(), map).getDesc());
            videoUploadDto.setAid(Long.parseLong(history.getAvId()));
            String republishRes = BiliApi.editPublish(biliBiliUser, videoUploadDto);
            log.info("重新投稿={}", republishRes);

            // 发布任务出队列
            TaskUtil.publishTask.remove(history.getId());
            Integer code = JsonPath.read(republishRes, "code");
            if (code == 0) {
                if (StringUtils.isNotBlank(wxuid) && StringUtils.isNotBlank(pushMsgTags) && pushMsgTags.contains("视频投稿")) {
                    Message message = new Message();
                    message.setAppToken(wxToken);
                    message.setContentType(Message.CONTENT_TYPE_TEXT);
                    message.setContent(WX_MSG_FORMAT.formatted("重新投稿成功", room.getUname(), room.getTitle(),
                            LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy年MM月dd日HH点mm分ss秒")), errMsg));
                    message.setUid(wxuid);
                    WxPusher.send(message);
                }
            } else {
                if (StringUtils.isNotBlank(wxuid) && StringUtils.isNotBlank(pushMsgTags) && pushMsgTags.contains("视频投稿")) {
                    Message message = new Message();
                    message.setAppToken(wxToken);
                    message.setContentType(Message.CONTENT_TYPE_TEXT);
                    message.setContent(WX_MSG_FORMAT.formatted("重新投稿失败", room.getUname(), room.getTitle(),
                            LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy年MM月dd日HH点mm分ss秒")),
                            JsonPath.read(republishRes, "message")));
                    message.setUid(wxuid);
                    WxPusher.send(message);
                }
            }
        } finally {
            TaskUtil.publishTask.remove(history.getId());
        }


    }

    public boolean publishRecordHistory(RecordHistory history) {
        if (history.isPublish()) {
            log.error("视频状态为已发布，退出==>{}", JSON.toJSONString(history));
            return false;
        }
        if (history.getUploadRetryCount() > 10) {
            log.error("视频发布重试次数过多，退出==>{}", JSON.toJSONString(history));
            return false;
        }
        Thread publishThread = TaskUtil.publishTask.get(history.getId());
        if (publishThread != null) {
            //正在发布，直接退出
            log.error("视频正在发布，退出==>{}", JSON.toJSONString(history));
            return false;
        }
        publishThread = TaskUtil.publishTask.get(history.getId());
        if (publishThread != null) {
            //正在发布，直接退出
            log.error("视频正在发布，直接退出==>{}", JSON.toJSONString(history));
            return false;
        }
        // 发布任务入队列
        TaskUtil.publishTask.put(history.getId(), Thread.currentThread());
        try {

            RecordRoom room = roomRepository.findByRoomId(history.getRoomId());
            String wxuid = room.getWxuid();
            String pushMsgTags = room.getPushMsgTags();
            log.info("发布视频事件开始：{}", room.getUname());

            if (room.getTid() == null) {
                //没有设置分区，直接取消上传
                TaskUtil.publishTask.remove(history.getId());
                log.error("视频没有设置分区，退出==>{}", JSON.toJSONString(history));
                return false;
            }
            List<RecordHistoryPart> uploadParts = partRepository.findByHistoryIdOrderByStartTimeAsc(history.getId());
            if (uploadParts.size() == 0) {
                log.info("发布视频事件分p不能为空，删除分p：{}", JSON.toJSONString(history));
                historyRepository.delete(history);
                TaskUtil.publishTask.remove(history.getId());
                return false;
            }
            if (uploadParts.size() > 100) {
                log.error("录制异常，该录制历史part数量已超过100，强制分次投稿");
                //更新唯一键,更新录制状态
                String eventId = history.getEventId();
                String sessionId = history.getSessionId();
                history.setEventId(eventId + 1);
                history.setSessionId(sessionId + 1);
                history = historyRepository.save(history);

                List<RecordHistoryPart> subList = uploadParts.subList(100, uploadParts.size());
                if (!subList.isEmpty()) {
                    //创建新的录制历史
                    history.setId(null);
                    history.setEventId(eventId + 2);
                    history.setSessionId(sessionId + 2);
                    history.setStartTime(subList.get(0).getStartTime());
                    history = historyRepository.save(history);
                    for (RecordHistoryPart part : subList) {
                        part.setHistoryId(history.getId());
                        partRepository.save(part);
                    }
                    if (StringUtils.isNotBlank(wxuid) && StringUtils.isNotBlank(pushMsgTags) && pushMsgTags.contains("视频投稿")) {
                        Message message = new Message();
                        message.setAppToken(wxToken);
                        message.setContentType(Message.CONTENT_TYPE_TEXT);
                        message.setContent(WX_MSG_FORMAT.formatted("投稿失败", room.getUname(), room.getTitle(),
                                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy年MM月dd日HH点mm分ss秒")),
                                "分p数量超过100,将在切割后再次投稿，当前分P数量为：" + uploadParts.size()));
                        message.setUid(wxuid);
                        WxPusher.send(message);
                    }
                    return false;
                }
            }
            LocalDateTime now = LocalDateTime.now();
            for (RecordHistoryPart uploadPart : uploadParts) {
                Optional<RecordHistoryPart> flsuhPartOptional = partRepository.findById(uploadPart.getId());
                uploadPart = flsuhPartOptional.get();
                String filePath = uploadPart.getFilePath().intern();
                File file = new File(filePath);
                //已经上传完成就跳过
                if (uploadPart.isUpload()) {
                    continue;
                }
                if (file.exists()) {
                    if (uploadPart.isRecording() && file.lastModified() > System.currentTimeMillis() - (10 * 60 * 1000)) {
                        log.error("发布视频事件错误，还有分p还在录制中==>{}", JSON.toJSONString(uploadPart));
                        TaskUtil.publishTask.remove(history.getId());
                        return false;
                    } else {
                        uploadPart.setRecording(false);
                        if (uploadPart.getFileSize() == 0 || uploadPart.getDuration() == 0) {
                            uploadPart.setFileSize(file.length());
                            uploadPart.setDuration((float)file.length() / 1024 / 1024);
                        }
                        uploadPart = partRepository.save(uploadPart);
                        if (uploadPart.getEndTime().isAfter(now.plusMinutes(11L))) {
                            log.error("发布视频事件错误，有分p结束时间在十分钟以内==>{}", JSON.toJSONString(uploadPart));
                            TaskUtil.publishTask.remove(history.getId());
                            return false;
                        }
                        if (uploadPart.getFileSize() < 1024 * 1024 * room.getFileSizeLimit()) {
                            log.error("文件大小小于设置的忽略大小，自动删除。");
                            partRepository.delete(uploadPart);
                            continue;
                        }
                        if (uploadPart.getDuration() < room.getDurationLimit()) {
                            log.error("文件时长小于设置的忽略时间，自动删除。");
                            partRepository.delete(uploadPart);
                            continue;
                        }
                    }
                }
                Thread thread = TaskUtil.partUploadTask.get(uploadPart.getId());
                if (thread != null && thread != Thread.currentThread()) {
                    //等待线程上传完成
                    log.info("partId={},{} ===>正在上传 ，等待上传完成在发布,即将等待获取锁", uploadPart.getId(), filePath);
                    synchronized (filePath) {
                        TaskUtil.partUploadTask.remove(uploadPart.getId());
                        log.error("视频发布流程获取part上传锁成功，即将再次检查是否已上传完成");
                        //再次检查是否上传完成
                        Optional<RecordHistoryPart> partOptional = partRepository.findById(uploadPart.getId());
                        if (partOptional.isPresent()) {
                            RecordHistoryPart part = partOptional.get();
                            if (!part.isUpload()) {
                                log.error("视频发布流程获取part上传锁成功，检查到未上传完成");
                                uploadServiceFactory.getUploadService(room.getLine()).upload(uploadPart);
                            }
                        }

                    }
                } else {
                    log.error("视频发布流程,检查到未上传完成,开始上传！");
                    uploadServiceFactory.getUploadService(room.getLine()).upload(uploadPart);
                }

            }
            int preSize = uploadParts.size();
            //重新加载上传列表
            uploadParts = partRepository.findByHistoryIdOrderByStartTimeAsc(history.getId());
            if (preSize != uploadParts.size()) {
                log.error("发布视频事件错误，分p数量发生变动==>{}", JSON.toJSONString(history));
                if (StringUtils.isNotBlank(wxuid) && StringUtils.isNotBlank(pushMsgTags) && pushMsgTags.contains("视频投稿")) {
                    Message message = new Message();
                    message.setAppToken(wxToken);
                    message.setContentType(Message.CONTENT_TYPE_TEXT);
                    message.setContent(WX_MSG_FORMAT.formatted("投稿失败", room.getUname(), room.getTitle(),
                            LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy年MM月dd日HH点mm分ss秒")),
                            "分p数量发生变动"));
                    message.setUid(wxuid);
                    WxPusher.send(message);
                }
                return false;
            }
            long count = uploadParts.stream().filter(RecordHistoryPart::isUpload).count();
            if (count != uploadParts.size()) {
                //没有全部上传完成返回失败
                TaskUtil.publishTask.remove(history.getId());
                return false;
            }
            if (room.isUpload()) {
                if (room.getUploadUserId() == null) {
                    log.info("视频发布事件，没有设置上传用户，无法发布 ==>{}", JSON.toJSONString(room));
                    TaskUtil.publishTask.remove(history.getId());
                    return false;
                } else {
                    Optional<BiliBiliUser> userOptional = biliUserRepository.findById(room.getUploadUserId());
                    if (!userOptional.isPresent()) {
                        log.error("视频发布事件，用户不存在，无法发布 ==>{}", JSON.toJSONString(room));
                        TaskUtil.publishTask.remove(history.getId());
                        return false;
                    }
                    BiliBiliUser biliBiliUser = userOptional.get();
                    if (!biliBiliUser.isLogin()) {
                        log.error("视频发布事件，用户登录状态失效，无法发布，请重新登录 ==>{}", JSON.toJSONString(room));
                        TaskUtil.publishTask.remove(history.getId());
                        return false;
                    }
                    // 检查是否已经过期，调用用户信息接口
                    WebCookie webCookie = Cookie.parse(biliBiliUser.getCookies());
                    UserMy userMy = new UserMy(webCookie);
                    UserMyRootBean myInfo = userMy.getPojo();
                    if (myInfo.getCode() == -101) {
                        biliBiliUser.setLogin(false);
                        biliBiliUser = biliUserRepository.save(biliBiliUser);
                        TaskUtil.publishTask.remove(history.getId());
                        if (StringUtils.isNotBlank(wxuid) && StringUtils.isNotBlank(pushMsgTags) && pushMsgTags.contains("视频投稿")) {
                            Message message = new Message();
                            message.setAppToken(wxToken);
                            message.setContentType(Message.CONTENT_TYPE_TEXT);
                            message.setContent(WX_MSG_FORMAT.formatted("投稿失败", room.getUname(), room.getTitle(),
                                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy年MM月dd日HH点mm分ss秒")),
                                    biliBiliUser.getUname() + "登录已过期，请重新登录"));
                            message.setUid(wxuid);
                            WxPusher.send(message);
                        }
                        throw new RuntimeException("{}登录已过期，请重新登录! " + biliBiliUser.getUname());
                    }


                    Map<String, Object> map = new HashMap<>();
                    LocalDateTime startTime = history.getStartTime();
                    map.put("date", startTime);

                    String uname = room.getUname();
                    if (uname == null) {
                        map.put("${uname}", "");
                    } else {
                        map.put("${uname}", uname);
                    }
                    String title = StringUtils.isNotBlank(history.getTitle()) ? history.getTitle() : "直播录像";
                    map.put("${title}", title);
                    map.put("${roomId}", room.getRoomId());
                    map.put("${areaName}", "");
                    List<SingleVideoDto> dtos = new ArrayList<>();
                    for (int i = 0; i < uploadParts.size(); i++) {
                        RecordHistoryPart uploadPart = uploadParts.get(i);
                        SingleVideoDto dto = new SingleVideoDto();
                        title = StringUtils.isNotBlank(uploadPart.getLiveTitle()) ? uploadPart.getLiveTitle() : "直播录像";
                        map.put("${title}", title);
                        map.put("date", uploadPart.getStartTime());
                        map.put("${index}", i + 1);
                        map.put("${areaName}", uploadPart.getAreaName());
                        String filePath = uploadPart.getFilePath();
                        String fileName = filePath.substring(filePath.lastIndexOf("/") + 1, filePath.lastIndexOf("."));
                        map.put("${fileName}", fileName);
                        dto.setTitle(this.template(room.getPartTitleTemplate(), map).getDesc());
                        //同步标题
                        uploadPart.setTitle(this.template(room.getPartTitleTemplate(), map).getDesc());
                        uploadPart = partRepository.save(uploadPart);
                        dto.setDesc("");
                        dto.setFilename(uploadPart.getFileName());
                        dtos.add(dto);
                    }
                    String coverUrl = room.getCoverUrl();
                    if ("live".equals(coverUrl)) {
                        try {
                            String filePath = uploadParts.get(uploadParts.size() - 1).getFilePath();
                            filePath = filePath.substring(0, filePath.lastIndexOf("."));
                            filePath += ".cover.jpg";
                            File cover = new File(filePath);
                            if (!cover.exists()) {
                                cover = new File(filePath.replaceAll(".cover.jpg", ".jpg"));
                            }
                            if (!cover.exists()) {
                                cover = new File(filePath.replaceAll(".cover.jpg", ".png"));
                            }
                            if (!cover.exists()) {
                                cover = new File(filePath.replaceAll(".cover.jpg", ".cover.png"));
                            }
                            byte[] bytes = new byte[(int)cover.length()];
                            FileInputStream inputStream = new FileInputStream(cover);
                            inputStream.read(bytes);
                            inputStream.close();
                            String uploadCoverResponse = BiliApi.uploadCover(biliBiliUser, cover.getName(), bytes);
                            log.info("clientPublish uploadCover==>{}", uploadCoverResponse);
                            coverUrl = JsonPath.read(uploadCoverResponse, "data.url");
                            history.setCoverUrl(coverUrl);
                            history = historyRepository.save(history);
                        } catch (Exception e) {
                            log.error("{}==>使用直播封面失败", room.getUname(), e);
                            coverUrl = "";
                        }
                    }
                    VideoUploadDto videoUploadDto = new VideoUploadDto();

                    map.put("date", startTime);
                    videoUploadDto.setTid(room.getTid());
                    videoUploadDto.setCover(coverUrl);
                    videoUploadDto.setCopyright(room.getCopyright());
                    videoUploadDto.setNo_disturbance(room.getIsOnlySelf());
                    videoUploadDto.setIs_only_self(room.getIsOnlySelf());
                    videoUploadDto.setTitle(this.template(room.getTitleTemplate(), map).getDesc());
                    if (videoUploadDto.getCopyright() == 2) {
                        videoUploadDto.setSource(this.template(videoUploadDto.getSource(), map).getDesc());
                    }
                    videoUploadDto.setDesc(this.template(room.getDescTemplate(), map).getDesc());
                    videoUploadDto.setDesc_v2(this.template(room.getDescTemplate(), map).getDescV2Dtos());
                    if (StringUtils.isNotBlank(room.getDynamicTemplate())) {
                        videoUploadDto.setDynamic(this.template(room.getDynamicTemplate(), map).getDesc());
                        videoUploadDto.setDynamic_v2(this.template(room.getDynamicTemplate(), map).getDescV2Dtos());
                    }
                    videoUploadDto.setVideos(dtos);
                    videoUploadDto.setTag(this.template(room.getTags(), map).getDesc());
                    String uploadRes = null;
                    try {
                        uploadRes = BiliApi.webPublish(biliBiliUser, videoUploadDto);
                        log.info("webPublish uploadRes==>{}", uploadRes);
                        if (uploadRes.contains("验证码")) {
                            Thread.sleep(120 * 1000L);
                            uploadRes = BiliApi.webPublish(biliBiliUser, videoUploadDto);
                            log.info("clientPublish uploadRes==>{}", uploadRes);
                        }
                        String bvid = JSON.parseObject(uploadRes).getJSONObject("data").getString("bvid");
                        String aid = JSON.parseObject(uploadRes).getJSONObject("data").getString("aid");
                        if (StringUtils.isBlank(bvid) || StringUtils.isBlank(aid)) {
                            log.info("发布={}=视频失败 == > {}", room.getUname(), uploadRes);
                            throw new RuntimeException(uploadRes);
                        }
                        history.setBvId(bvid);
                        history.setAvId(aid);
                        history.setPublish(true);
                        history = historyRepository.save(history);
                        log.info("发布={}=视频成功 == > {}", room.getUname(), JSON.toJSONString(history));
                        try {
                            if (room.getSeasonId() != null && room.getSeasonId() > 0) {
                                String addSeasons = BiliApi.addSeasons(biliBiliUser, room.getSeasonId(), aid, String.valueOf(uploadParts.get(0).getCid()), videoUploadDto.getTitle());
                                Integer code = JsonPath.read(addSeasons, "code");
                                if (code == 0) {
                                    log.info("{}=加入合集成功 == > {}", history.getTitle(), JSON.toJSONString(history));
                                }
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                            log.error("{}=加入合集失败 == > {}", history.getTitle(), JSON.toJSONString(history));
                        }
                        if (StringUtils.isNotBlank(wxuid) && StringUtils.isNotBlank(pushMsgTags) && pushMsgTags.contains("视频投稿")) {
                            Message message = new Message();
                            message.setAppToken(wxToken);
                            message.setContentType(Message.CONTENT_TYPE_TEXT);
                            message.setContent(WX_MSG_FORMAT.formatted("投稿成功", room.getUname(), room.getTitle(),
                                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy年MM月dd日HH点mm分ss秒")), "bvid=>" + bvid));
                            message.setUid(wxuid);
                            WxPusher.send(message);
                        }

                        //如果配置成投稿完成后删除则删除文件
                        try {
                            for (RecordHistoryPart part : uploadParts) {
                                String filePath = part.getFilePath();
                                if (room.getDeleteType() == 9) {
                                    File file = new File(filePath);
                                    boolean delete = file.delete();
                                    if (delete) {
                                        log.error("{}=>文件删除成功！！！", filePath);
                                    } else {
                                        log.error("{}=>文件删除失败！！！", filePath);
                                    }
                                } else if (StringUtils.isNotBlank(room.getMoveDir()) && room.getDeleteType() == 10) {

                                    String fileName = filePath.substring(filePath.lastIndexOf("/") + 1, filePath.lastIndexOf("."));
                                    String startDirPath = filePath.substring(0, filePath.lastIndexOf('/') + 1);
                                    String toDirPath = room.getMoveDir() + filePath.substring(0, filePath.lastIndexOf('/') + 1).replace(workPath, "");
                                    File toDir = new File(toDirPath);
                                    if (!toDir.exists()) {
                                        toDir.mkdirs();
                                    }
                                    File startDir = new File(startDirPath);
                                    File[] files = startDir.listFiles((file, s) -> s.startsWith(fileName));
                                    if (files != null) {
                                        for (File file : files) {
                                            if (!filePath.startsWith(workPath)) {
                                                part.setFileDelete(true);
                                                part = partRepository.save(part);
                                                continue;
                                            }
                                            try {
                                                Files.move(Paths.get(file.getPath()), Paths.get(toDirPath + file.getName()),
                                                        StandardCopyOption.REPLACE_EXISTING);
                                                log.error("{}=>文件移动成功！！！", file.getName());
                                            } catch (Exception e) {
                                                log.error("{}=>文件移动失败！！！", file.getName());
                                            }
                                        }
                                    }

                                    part.setFilePath(toDirPath + filePath.substring(filePath.lastIndexOf("/") + 1));
                                    part.setFileDelete(true);
                                    part = partRepository.save(part);
                                }
                            }
                        } catch (Exception de) {
                            de.printStackTrace();
                            log.error("投稿成功后发生处理文件删除移动发生异常：", de);
                        }

                    } catch (Exception e) {
                        history.setUploadRetryCount(history.getUploadRetryCount() + 1);
                        history = historyRepository.save(history);
                        log.info("发布={}=视频失败 == > {}", room.getUname(), JSON.toJSONString(history), e);
                        if (StringUtils.isNotBlank(wxuid) && StringUtils.isNotBlank(pushMsgTags) && pushMsgTags.contains("视频投稿")) {
                            Message message = new Message();
                            message.setAppToken(wxToken);
                            message.setContentType(Message.CONTENT_TYPE_TEXT);
                            message.setContent(WX_MSG_FORMAT.formatted("投稿失败", room.getUname(), room.getTitle(),
                                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy年MM月dd日HH点mm分ss秒")), uploadRes != null ? uploadRes : e.getMessage()));
                            message.setUid(wxuid);
                            WxPusher.send(message);
                        }
                    } finally {
                        TaskUtil.publishTask.remove(history.getId());
                    }
                }
            }
        } catch (Exception e) {
            log.error("投稿发生异常：", e);
        } finally {
            TaskUtil.publishTask.remove(history.getId());
        }
        return true;
    }

    public DescDto template(String template, Map<String, Object> map) {
        List<DescV2Dto> resultList = new ArrayList<>();
        StringBuilder desc = new StringBuilder();
        List<String> stringList = splitTemplateByUid(template);
        for (String s : stringList) {
            if (s.startsWith("${@")) {
                long uid = Long.parseLong(s.substring(3, s.length() - 1));
                try {
                    BiliApi.BiliUserCardResponseDto userCard = BiliApi.getUserCard(uid);
                    if (userCard != null && userCard.getCode() == 0) {
                        //必须带个空格，否则报错简介过长
                        desc.append("@").append(userCard.getCard().getName() + " ");
                        DescV2Dto descV2Dto = new DescV2Dto();
                        descV2Dto.setBiz_id(String.valueOf(uid));
                        descV2Dto.setRaw_text(userCard.getCard().getName());
                        descV2Dto.setType(2);
                        resultList.add(descV2Dto);
                    }

                } catch (Exception e) {
                    log.error("@用户模板失败：{}", uid, e);
                }
            } else {
                s = s.replace("${uname}", map.get("${uname}") != null ? map.get("${uname}").toString() : "")
                        .replace("${title}", map.get("${title}") != null ? map.get("${title}").toString() : "")
                        .replace("${index}", map.get("${index}") != null ? map.get("${index}").toString() : "")
                        .replace("${areaName}", map.get("${areaName}") != null ? map.get("${areaName}").toString() : "")
                        .replace("${roomId}", map.get("${roomId}") != null ? map.get("${roomId}").toString() : "");
                if (s.contains("${")) {
                    try {
                        LocalDateTime localDateTime = (LocalDateTime)map.get("date");
                        String substring = s.substring(s.indexOf("${"));
                        String date = substring.substring(0, substring.indexOf("}") + 1);
                        String format = localDateTime.format(DateTimeFormatter.ofPattern(date.substring(2, date.length() - 1)));
                        s = s.replace(date, format);
                    } catch (Exception e) {
                        log.error("时间格式模板失败：{}", template);
                    }
                }
                s = s.replace(",,", ",");

                DescV2Dto descV2Dto = new DescV2Dto();
                descV2Dto.setRaw_text(s);
                descV2Dto.setType(1);
                resultList.add(descV2Dto);
                desc.append(s);
            }

        }
        return new DescDto(desc.toString(), resultList);
    }

    @Data
    @AllArgsConstructor
    class DescDto {
        public final String desc;
        public final List<DescV2Dto> descV2Dtos;
    }
}
