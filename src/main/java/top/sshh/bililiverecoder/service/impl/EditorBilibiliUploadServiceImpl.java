package top.sshh.bililiverecoder.service.impl;

import com.alibaba.fastjson.JSON;
import com.zjiecode.wxpusher.client.WxPusher;
import com.zjiecode.wxpusher.client.bean.Message;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import top.sshh.bililiverecoder.entity.BiliBiliUser;
import top.sshh.bililiverecoder.entity.RecordHistory;
import top.sshh.bililiverecoder.entity.RecordHistoryPart;
import top.sshh.bililiverecoder.entity.RecordRoom;
import top.sshh.bililiverecoder.repo.BiliUserRepository;
import top.sshh.bililiverecoder.repo.RecordHistoryPartRepository;
import top.sshh.bililiverecoder.repo.RecordHistoryRepository;
import top.sshh.bililiverecoder.repo.RecordRoomRepository;
import top.sshh.bililiverecoder.service.RecordPartUploadService;
import top.sshh.bililiverecoder.util.TaskUtil;
import top.sshh.bililiverecoder.util.UploadEnums;
import top.sshh.bililiverecoder.util.bili.Cookie;
import top.sshh.bililiverecoder.util.bili.WebCookie;
import top.sshh.bililiverecoder.util.bili.upload.EditorChunkUploadRequest;
import top.sshh.bililiverecoder.util.bili.upload.EditorPreUploadRequest;
import top.sshh.bililiverecoder.util.bili.upload.EdtiorCompleteUploadRequest;
import top.sshh.bililiverecoder.util.bili.upload.EdtiorTranscodeRequest;
import top.sshh.bililiverecoder.util.bili.upload.pojo.CompleteUploadBean;
import top.sshh.bililiverecoder.util.bili.upload.pojo.EditorPreUploadBean;
import top.sshh.bililiverecoder.util.bili.user.UserMy;
import top.sshh.bililiverecoder.util.bili.user.UserMyRootBean;

import java.io.File;
import java.io.RandomAccessFile;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Service("editorBilibiliUploadService")
public class EditorBilibiliUploadServiceImpl implements RecordPartUploadService {

    public static final String OS = "editor";

    @Value("${record.wx-push-token}")
    private String wxToken;
    private static final String WX_MSG_FORMAT = """
            上传结果: %s
            收到主播%s云剪辑上传%s事件
            房间名: %s
            时间: %s
            文件路径: %s
            文件录制开始时间: %s
            文件录制时长: %s 分钟
            文件录制大小: %.3f GB
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
        try {
            RecordRoom room = roomRepository.findByRoomId(part.getRoomId());

            if (room != null) {
                UploadEnums uploadEnums = UploadEnums.find(room.getLine());
                String wxuid = room.getWxuid();
                String pushMsgTags = room.getPushMsgTags();
                // 上传任务入队列
                String filePath = part.getFilePath().intern();
                File uploadFile = new File(filePath);
                if (!uploadFile.exists()) {
                    log.error("分片上传失败，文件不存在==>{}", filePath);
                    return;
                }
                synchronized (filePath) {
                    Optional<RecordHistory> historyOptional = historyRepository.findById(part.getHistoryId());
                    if (!historyOptional.isPresent()) {
                        log.error("分片上传失败，history不存在==>{}", JSON.toJSONString(part));
                        TaskUtil.partUploadTask.remove(part.getId());
                        return;
                    }
                    RecordHistory history = historyOptional.get();
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
                        // 检查是否已经过期，调用用户信息接口
                        // 登录验证结束
                        WebCookie webCookie = Cookie.parse(biliBiliUser.getCookies());
                        UserMy userMy = new UserMy(webCookie);
                        UserMyRootBean myInfo = userMy.getPojo();
                        if (myInfo.getCode() == -101) {
                            biliBiliUser.setLogin(false);
                            biliBiliUser = biliUserRepository.save(biliBiliUser);
                            TaskUtil.partUploadTask.remove(part.getId());
                            if (StringUtils.isNotBlank(wxuid) && StringUtils.isNotBlank(pushMsgTags) && pushMsgTags.contains("云剪辑")) {
                                Message message = new Message();
                                message.setAppToken(wxToken);
                                message.setContentType(Message.CONTENT_TYPE_TEXT);
                                message.setContent(WX_MSG_FORMAT.formatted("上传失败", room.getUname(), "开始", room.getTitle(),
                                        LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy年MM月dd日HH点mm分ss秒")),
                                        part.getFilePath(), part.getStartTime().format(DateTimeFormatter.ofPattern("yyyy年MM月dd日HH点mm分ss秒")), (int)part.getDuration() / 60, ((float)part.getFileSize() / 1024 / 1024 / 1024), biliBiliUser.getUname() + "登录已过期，请重新登录\n" + "线路：" + uploadEnums.getLine()));
                                message.setUid(wxuid);
                                WxPusher.send(message);
                            }
                            throw new RuntimeException("{}登录已过期，请重新登录! " + biliBiliUser.getUname());
                        }
                        // 登录验证结束
                        long fileSize = uploadFile.length();
                        Map<String, String> preParams = new HashMap<>();
                        if(StringUtils.isNotBlank(part.getTitle())){
                            preParams.put("name", room.getUname()+part.getTitle());
                        }else {
                            preParams.put("name", room.getUname()+uploadFile.getName());
                        }
                        preParams.put("resource_file_type", "flv");
                        preParams.put("size", String.valueOf(fileSize));
                        EditorPreUploadRequest preuploadRequest = new EditorPreUploadRequest(webCookie, preParams);
                        EditorPreUploadBean preUploadBean = preuploadRequest.getPojo();
                        // 分段上传\
                        Long chunkSize = preUploadBean.getData().getPer_size();
                        int chunkNum = (int)Math.ceil((double)fileSize / chunkSize);
                        AtomicInteger upCount = new AtomicInteger(0);
                        AtomicInteger tryCount = new AtomicInteger(0);
                        String[] etagArray = new String[chunkNum];
                        List<Runnable> runnableList = new ArrayList<>();
                        for (int i = 0; i < chunkNum; i++) {
                            int finalI = i;
                            Runnable runnable = () -> {
                                try {
                                    while (tryCount.get() < 200) {
                                        try {
                                            // 上传
                                            long endSize = (finalI + 1) * chunkSize;
                                            long finalChunkSize = chunkSize;
                                            Map<String, String> chunkParams = new HashMap<>();
                                            chunkParams.put("index", String.valueOf(finalI));
                                            chunkParams.put("size", String.valueOf(finalChunkSize));
                                            chunkParams.put("start", String.valueOf(finalI * finalChunkSize));
                                            chunkParams.put("end", String.valueOf(endSize));
                                            if (endSize > fileSize) {
                                                endSize = fileSize;
                                                finalChunkSize = fileSize - (finalI * finalChunkSize);
                                                chunkParams.put("size", String.valueOf(finalChunkSize));
                                                chunkParams.put("end", String.valueOf(endSize));
                                            }
                                            EditorChunkUploadRequest chunkUploadRequest = new EditorChunkUploadRequest(preUploadBean, chunkParams, new RandomAccessFile(filePath, "r"));
                                            String etag = chunkUploadRequest.getPage();
                                            etagArray[finalI]=etag;
                                            int count = upCount.incrementAndGet();
                                            log.info("{}==>[{}] 上传视频part {} 进度{}/{}", Thread.currentThread().getName(), room.getTitle(),
                                                    filePath, count, chunkNum);
                                            break;
                                        } catch (Exception e) {
                                            tryCount.incrementAndGet();
                                            log.info("{}==>[{}] 上传视频part {}, index {}, size {}, start {}, end {}, exception={}", Thread.currentThread().getName(), room.getTitle(),
                                                    filePath, finalI, chunkSize, finalI * chunkSize, (finalI + 1) * chunkSize, ExceptionUtils.getStackTrace(e));
                                            try {
                                                //                                                log.info("上传失败等待十秒==>{}", uploadFile.getName());
                                                Thread.sleep(10000L);
                                            } catch (InterruptedException ex) {
                                                ex.printStackTrace();
                                            }
                                        }
                                    }
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            };

                            runnableList.add(runnable);

                        }

                        //并发上传
                        Message message = new Message();
                        if (StringUtils.isNotBlank(wxuid) && StringUtils.isNotBlank(pushMsgTags) && pushMsgTags.contains("云剪辑")) {
                            message.setAppToken(wxToken);
                            message.setContentType(Message.CONTENT_TYPE_TEXT);
                            message.setContent(WX_MSG_FORMAT.formatted("开始上传", room.getUname(), "开始", room.getTitle(),
                                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy年MM月dd日HH点mm分ss秒")),
                                    part.getFilePath(), part.getStartTime().format(DateTimeFormatter.ofPattern("yyyy年MM月dd日HH点mm分ss秒")), (int)part.getDuration() / 60, ((float)part.getFileSize() / 1024 / 1024 / 1024), biliBiliUser.getUname() + "\n线路：无"));
                            message.setUid(wxuid);
                            WxPusher.send(message);
                        }

                        runnableList.stream().parallel().forEach(Runnable::run);
                        if (tryCount.get() >= 200) {
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
                            if (StringUtils.isNotBlank(wxuid) && StringUtils.isNotBlank(pushMsgTags) && pushMsgTags.contains("云剪辑")) {
                                message.setAppToken(wxToken);
                                message.setContentType(Message.CONTENT_TYPE_TEXT);
                                message.setContent(WX_MSG_FORMAT.formatted("上传失败", room.getUname(), "开始", room.getTitle(),
                                        LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy年MM月dd日HH点mm分ss秒")),
                                        part.getFilePath(), part.getStartTime().format(DateTimeFormatter.ofPattern("yyyy年MM月dd日HH点mm分ss秒")), (int)part.getDuration() / 60, ((float)part.getFileSize() / 1024 / 1024 / 1024), biliBiliUser.getUname() + "并发上传失败，存在异常\n" + "线路：" + uploadEnums.getLine()));
                                message.setUid(wxuid);
                                WxPusher.send(message);
                            }
                            throw new RuntimeException(part.getFileName() + "===并发上传失败，存在异常");
                        }
                        //通知服务器上传完成
                        userOptional = biliUserRepository.findById(room.getUploadUserId());
                        biliBiliUser = userOptional.get();
                        webCookie = Cookie.parse(biliBiliUser.getCookies());
                        Map<String, String> completeParams = new HashMap<>();
                        completeParams.put("etags", String.join(",", etagArray));
                        EdtiorCompleteUploadRequest completeUploadRequest = new EdtiorCompleteUploadRequest(webCookie, preUploadBean, completeParams);
                        CompleteUploadBean completeUploadBean = completeUploadRequest.getPojo();
                        log.info("{}，云剪辑上传完成，==>{}",part.getTitle(),JSON.toJSONString(completeUploadBean));
                        try {
                            //等待五秒在开始转码
                            Thread.sleep(5000L);
                        }catch (Exception ignored){}
                        EdtiorTranscodeRequest transcodeRequest = new EdtiorTranscodeRequest(webCookie, preUploadBean);
                        String page = transcodeRequest.getPage();
                        log.info("{}，云剪辑转码请求完成，==>{}",part.getTitle(),page);
                        if (Integer.valueOf(0).equals(completeUploadBean.getCode())) {
                            if (StringUtils.isNotBlank(wxuid) && StringUtils.isNotBlank(pushMsgTags) && pushMsgTags.contains("云剪辑")) {
                                message.setAppToken(wxToken);
                                message.setContentType(Message.CONTENT_TYPE_TEXT);
                                message.setContent(WX_MSG_FORMAT.formatted("上传成功", room.getUname(), "结束", room.getTitle(),
                                        LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy年MM月dd日HH点mm分ss秒")),
                                        part.getFilePath(), part.getStartTime().format(DateTimeFormatter.ofPattern("yyyy年MM月dd日HH点mm分ss秒")), (int)part.getDuration() / 60, ((float)part.getFileSize() / 1024 / 1024 / 1024), ""));
                                message.setUid(wxuid);
                                WxPusher.send(message);
                            }
                        } else {
                            if (StringUtils.isNotBlank(wxuid) && StringUtils.isNotBlank(pushMsgTags) && pushMsgTags.contains("云剪辑")) {
                                message.setAppToken(wxToken);
                                message.setContentType(Message.CONTENT_TYPE_TEXT);
                                message.setContent(WX_MSG_FORMAT.formatted("上传失败", room.getUname(), "结束", room.getTitle(),
                                        LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy年MM月dd日HH点mm分ss秒")),
                                        part.getFilePath(), part.getStartTime().format(DateTimeFormatter.ofPattern("yyyy年MM月dd日HH点mm分ss秒")), (int)part.getDuration() / 60, ((float)part.getFileSize() / 1024 / 1024 / 1024), ""));
                                message.setUid(wxuid);
                                WxPusher.send(message);
                            }
                        }
                    }
                }

            }
        } catch (Exception e) {
            log.error("云剪辑上传发送错误", e);
        } finally {
            TaskUtil.partUploadTask.remove(part.getId());
        }


    }
}
