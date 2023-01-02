package top.sshh.bililiverecoder.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.jayway.jsonpath.JsonPath;
import com.zjiecode.wxpusher.client.WxPusher;
import com.zjiecode.wxpusher.client.bean.Message;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import top.sshh.bililiverecoder.entity.BiliBiliUser;
import top.sshh.bililiverecoder.entity.RecordHistory;
import top.sshh.bililiverecoder.entity.RecordHistoryPart;
import top.sshh.bililiverecoder.entity.RecordRoom;
import top.sshh.bililiverecoder.repo.BiliUserRepository;
import top.sshh.bililiverecoder.repo.RecordHistoryPartRepository;
import top.sshh.bililiverecoder.repo.RecordHistoryRepository;
import top.sshh.bililiverecoder.repo.RecordRoomRepository;
import top.sshh.bililiverecoder.service.RecordPartUploadService;
import top.sshh.bililiverecoder.util.BiliApi;
import top.sshh.bililiverecoder.util.TaskUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.RandomAccessFile;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Component
public class RecordPartBilibiliUploadService implements RecordPartUploadService {

    @Value("${record.wx-push-token}")
    private String wxToken;
    private static final String WX_MSG_FORMAT= """
            收到主播%s分P上传%s事件
            房间名: %s
            时间: %s
            文件路径: %s
            文件录制开始时间: %s
            文件录制时长: %s 分钟
            文件录制大小: %.3f GB
            上传结果: %s
            原因: %s
            """;
    @Autowired
    private BiliUserRepository biliUserRepository;
    @Autowired
    private RecordHistoryPartRepository partRepository;
    @Autowired
    private RecordHistoryRepository historyRepository;
    @Autowired
    private RecordRoomRepository roomRepository;

    @Override
    public void asyncUpload(RecordHistoryPart part) {
        log.info("partId={},异步上传任务开始==>{}", part.getId(), part.getFilePath());
        this.upload(part);
    }

    @Override
    public void upload(RecordHistoryPart part) {
        Thread thread = TaskUtil.partUploadTask.get(part.getId());
        if (thread != null && thread != Thread.currentThread()) {
            log.info("当前线程为{} ,partId={}该文件正在被{}线程上传", Thread.currentThread(), part.getId(), thread.getName());
            return;
        }
        TaskUtil.partUploadTask.put(part.getId(), Thread.currentThread());
        RecordRoom room = roomRepository.findByRoomId(part.getRoomId());


        if (room != null) {
            String wxuid = room.getWxuid();
            String pushMsgTags = room.getPushMsgTags();
            if (room.getTid() == null) {
                //没有设置分区，直接取消上传
                return;
            }
            // 上传任务入队列
            String filePath = part.getFilePath().intern();
            synchronized (filePath) {
                Optional<RecordHistory> historyOptional = historyRepository.findById(part.getHistoryId());
                if (!historyOptional.isPresent()) {
                    log.error("分片上传失败，history不存在==>{}", JSON.toJSONString(part));
                    TaskUtil.partUploadTask.remove(part.getId());
                    return;
                }
                RecordHistory history = historyOptional.get();
                if (history.isUpload()) {
                    if (room.getUploadUserId() == null) {
                        log.info("分片上传事件，没有设置上传用户，无法上传 ==>{}", JSON.toJSONString(room));
                        TaskUtil.partUploadTask.remove(part.getId());
                        return;
                    } else {
                        Optional<BiliBiliUser> userOptional = biliUserRepository.findById(room.getUploadUserId());
                        if (!userOptional.isPresent()) {
                            log.error("分片上传事件，上传用户不存在，无法上传 ==>{}", JSON.toJSONString(room));
                            TaskUtil.partUploadTask.remove(part.getId());
                            return;
                        }
                        BiliBiliUser biliBiliUser = userOptional.get();
                        if (!biliBiliUser.isLogin()) {
                            log.error("分片上传事件，用户登录状态失效，无法上传，请重新登录 ==>{}", JSON.toJSONString(room));
                            TaskUtil.partUploadTask.remove(part.getId());
                            return;
                        }
                        boolean expired = false;
                        // 检查是否已经过期，调用用户信息接口
                        try {
                            String myInfo = BiliApi.appMyInfo(biliBiliUser);
                            String uname = JsonPath.read(myInfo, "data.uname");
                            if (StringUtils.isBlank(uname)) {
                                expired = true;
                            }
                        } catch (Exception e) {
                            expired = true;
                        }
                        if (expired) {
                            biliBiliUser.setLogin(false);
                            biliBiliUser = biliUserRepository.save(biliBiliUser);
                            TaskUtil.partUploadTask.remove(part.getId());
                            if(StringUtils.isNotBlank(wxuid)&&StringUtils.isNotBlank(pushMsgTags)&&pushMsgTags.contains("分P上传")){
                                Message message = new Message();
                                message.setAppToken(wxToken);
                                message.setContentType(Message.CONTENT_TYPE_TEXT);
                                message.setContent(WX_MSG_FORMAT.formatted(room.getUname(), "开始", room.getTitle(),
                                        LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy年MM月dd日HH点mm分ss秒")),
                                        part.getFilePath(), part.getStartTime().format(DateTimeFormatter.ofPattern("yyyy年MM月dd日HH点mm分ss秒")), (int) part.getDuration() / 60, ((float) part.getFileSize() / 1024 / 1024 / 1024), "上传失败", biliBiliUser.getUname() + "登录已过期，请重新登录"));
                                message.setUid(wxuid);
                                WxPusher.send(message);
                            }
                            throw new RuntimeException("{}登录已过期，请重新登录! " + biliBiliUser.getUname());
                        }
                        // 登录验证结束
                        String preRes = BiliApi.preUpload(biliBiliUser.getAccessToken(), biliBiliUser.getUid(), "ugcfr/pc3");
                        JSONObject preResObj = JSON.parseObject(preRes);
                        String url = preResObj.getString("url");
                        String complete = preResObj.getString("complete");
                        String filename = preResObj.getString("filename");
                        // 分段上传
                        File uploadFile = new File(filePath);
                        long fileSize = uploadFile.length();
                        long chunkSize = 1024 * 1024 * 5;
                        long chunkNum = (long) Math.ceil((double) fileSize / chunkSize);
                        AtomicInteger upCount = new AtomicInteger(0);
                        AtomicBoolean isThrow = new AtomicBoolean(false);
                        List<Runnable> runnableList = new ArrayList<>();
                        for (int i = 0; i < chunkNum; i++) {
                            int finalI = i;
                            Runnable runnable = () -> {
                                try (RandomAccessFile r = new RandomAccessFile(filePath, "r")) {
                                    int tryCount = 0;
                                    while (tryCount < 5) {
                                        try {
                                            // 上传
                                            String s = BiliApi.uploadChunk(url, filename, r, chunkSize,
                                                    finalI + 1, (int) chunkNum);
                                            if (!s.contains("OK")) {
                                                throw new RuntimeException("上传返回异常");
                                            }
                                            int count = upCount.incrementAndGet();
                                            log.info("{}==>[{}] 上传视频part {} 进度{}/{}, resp={}", Thread.currentThread().getName(), room.getTitle(),
                                                    filePath, count, chunkNum, s);
                                            tryCount = 5;
                                            isThrow.set(false);
                                        } catch (Exception e) {
                                            int count = upCount.get();
                                            log.info("{}==>[{}] 上传视频part {} 进度{}/{}, exception={}", Thread.currentThread().getName(), room.getTitle(),
                                                    filePath, count, chunkNum, ExceptionUtils.getStackTrace(e));
                                            isThrow.set(true);
                                        }
                                    }
                                } catch (Exception e) {
                                    e.printStackTrace();
                                    isThrow.set(true);
                                }
                            };

                            runnableList.add(runnable);

                        }

                        //并发上传

                        Message message = new Message();
                        message.setAppToken(wxToken);
                        message.setContentType(Message.CONTENT_TYPE_TEXT);
                        message.setContent(WX_MSG_FORMAT.formatted(room.getUname(), "开始", room.getTitle(),
                                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy年MM月dd日HH点mm分ss秒")),
                                part.getFilePath(), part.getStartTime().format(DateTimeFormatter.ofPattern("yyyy年MM月dd日HH点mm分ss秒")), (int) part.getDuration() / 60, ((float) part.getFileSize() / 1024 / 1024 / 1024), "开始上传", biliBiliUser.getUname()));
                        message.setUid(wxuid);
                        WxPusher.send(message);

                        runnableList.stream().parallel().forEach(Runnable::run);
                        if (isThrow.get()) {
                            part.setUpload(false);
                            part = partRepository.save(part);
                            historyOptional = historyRepository.findById(history.getId());
                            if (historyOptional.isPresent()) {
                                history = historyOptional.get();
                                history.setUploadRetryCount(history.getUploadRetryCount() + 1);
                                history = historyRepository.save(history);
                            }
                            //存在异常
                            TaskUtil.partUploadTask.remove(part.getId());
                            if(StringUtils.isNotBlank(wxuid)&&StringUtils.isNotBlank(pushMsgTags)&&pushMsgTags.contains("分P上传")){
                                message.setAppToken(wxToken);
                                message.setContentType(Message.CONTENT_TYPE_TEXT);
                                message.setContent(WX_MSG_FORMAT.formatted(room.getUname(), "开始", room.getTitle(),
                                        LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy年MM月dd日HH点mm分ss秒")),
                                        part.getFilePath(), part.getStartTime().format(DateTimeFormatter.ofPattern("yyyy年MM月dd日HH点mm分ss秒")), (int) part.getDuration() / 60, ((float) part.getFileSize() / 1024 / 1024 / 1024), "上传失败", biliBiliUser.getUname() + "并发上传失败，存在异常"));
                                message.setUid(wxuid);
                                WxPusher.send(message);
                            }
                            throw new RuntimeException("partId={}===并发上传失败，存在异常");
                        }

                        try {
                            FileInputStream stream = new FileInputStream(uploadFile);
                            String md5 = DigestUtils.md5Hex(stream).toLowerCase();
                            stream.close();
                            BiliApi.completeUpload(complete, (int) chunkNum, fileSize, md5,
                                    uploadFile.getName(), "2.0.0.1054");
                            part.setFileName(filename);
                            part.setUpload(true);
                            part.setUpdateTime(LocalDateTime.now());
                            part = partRepository.save(part);
                            //如果配置上传完成删除，则删除文件
                            if (room.getDeleteType()==1) {
                                boolean delete = uploadFile.delete();
                                if (delete) {
                                    log.error("{}=>文件删除成功！！！", filePath);
                                } else {
                                    log.error("{}=>文件删除失败！！！", filePath);
                                }
                            }
                            TaskUtil.partUploadTask.remove(part.getId());
                            log.info("partId={},文件上传成功==>{}", part.getId(), filePath);

                            if(StringUtils.isNotBlank(wxuid)&&StringUtils.isNotBlank(pushMsgTags)&&pushMsgTags.contains("分P上传")){
                                message.setAppToken(wxToken);
                                message.setContentType(Message.CONTENT_TYPE_TEXT);
                                message.setContent(WX_MSG_FORMAT.formatted(room.getUname(), "结束", room.getTitle(),
                                        LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy年MM月dd日HH点mm分ss秒")),
                                        part.getFilePath(), part.getStartTime().format(DateTimeFormatter.ofPattern("yyyy年MM月dd日HH点mm分ss秒")), (int) part.getDuration() / 60, ((float) part.getFileSize() / 1024 / 1024 / 1024), "上传成功", "服务器文件名称\n" + part.getFileName()));
                                message.setUid(wxuid);
                                WxPusher.send(message);
                            }
                        } catch (Exception e) {
                            //存在异常
                            TaskUtil.partUploadTask.remove(part.getId());
                            log.error("partId={},文件上传失败==>{}", part.getId(), filePath, e);
                            if(StringUtils.isNotBlank(wxuid)&&StringUtils.isNotBlank(pushMsgTags)&&pushMsgTags.contains("分P上传")){
                                message.setAppToken(wxToken);
                                message.setContentType(Message.CONTENT_TYPE_TEXT);
                                message.setContent(WX_MSG_FORMAT.formatted(room.getUname(), "结束", room.getTitle(),
                                        LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy年MM月dd日HH点mm分ss秒")),
                                        part.getFilePath(), part.getStartTime().format(DateTimeFormatter.ofPattern("yyyy年MM月dd日HH点mm分ss秒")), (int) part.getDuration() / 60, ((float) part.getFileSize() / 1024 / 1024 / 1024), "上传失败", e.getMessage()));
                                message.setUid(wxuid);
                                WxPusher.send(message);
                            }
                        }
                    }
                } else {
                    log.info("分片上传事件，文件不需要上传 ==>{}", JSON.toJSONString(part));
                    TaskUtil.partUploadTask.remove(part.getId());
                    return;
                }
            }

        }

    }
}
