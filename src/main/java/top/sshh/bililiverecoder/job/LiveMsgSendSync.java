package top.sshh.bililiverecoder.job;

import com.alibaba.fastjson.JSON;
import com.zjiecode.wxpusher.client.WxPusher;
import com.zjiecode.wxpusher.client.bean.Message;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import top.sshh.bililiverecoder.entity.*;
import top.sshh.bililiverecoder.entity.data.BiliReply;
import top.sshh.bililiverecoder.entity.data.BiliReplyResponse;
import top.sshh.bililiverecoder.repo.*;
import top.sshh.bililiverecoder.service.impl.LiveMsgService;
import top.sshh.bililiverecoder.util.BiliApi;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

@Slf4j
@Component
public class LiveMsgSendSync {

    @Value("${record.wx-push-token}")
    private String wxToken;
    private static final String WX_MSG_FORMAT= """
            收到弹幕评论发送事件
            主播名: %s
            房间名: %s
            BV号: %s
            时间: %s
            发送内容: %s
            发送结果: %s
            原因: %s
            """;
    @Autowired
    private BiliUserRepository userRepository;

    @Autowired
    private LiveMsgRepository msgRepository;

    @Autowired
    private RecordHistoryRepository historyRepository;

    @Autowired
    private RecordHistoryPartRepository partRepository;

    @Autowired
    private RecordRoomRepository roomRepository;

    @Autowired
    private LiveMsgService liveMsgService;

    private static final Lock lock = new ReentrantLock();

    @Scheduled(fixedDelay = 60000, initialDelay = 5000)
    public void sndMsgProcess() {
        log.info("发送弹幕定时任务开始");
        long startTime = System.currentTimeMillis();
        List<RecordHistory> historyList = historyRepository.findByPublishIsTrueAndCode(0);
        if (CollectionUtils.isEmpty(historyList)) {
            return;
        }

        DateFormat format = new SimpleDateFormat("HH:mm:ss");
        format.setTimeZone(TimeZone.getTimeZone("UTC"));
        List<RecordHistoryPart> partList = new ArrayList<>();
        for (RecordHistory history : historyList) {
            List<RecordHistoryPart> parts = partRepository.findByHistoryIdAndCidIsNotNullOrderByPageAsc(history.getId());
            //如果没有发送评论
            if(!history.isSendReply()) {
                RecordRoom room = roomRepository.findByRoomId(history.getRoomId());
                String wxuid = null;
                String pushMsgTags = null;
                BiliBiliUser user = null;
                if (room != null) {
                    wxuid = room.getWxuid();
                    pushMsgTags = room.getPushMsgTags();
                    Long uploadUserId = room.getUploadUserId();
                    Optional<BiliBiliUser> userOptional = userRepository.findById(uploadUserId);
                    if (userOptional.isPresent()) {
                        user = userOptional.get();
                        if (!(user.isLogin() && user.isEnable())) {
                            continue;
                        }
                    }
                }
                List<BiliReply> replies = new ArrayList<>();
                StringBuilder context = new StringBuilder();
                context.append("sc和上舰列表,网页可跳转\n");
                for (RecordHistoryPart part : parts) {
                    List<LiveMsg> msgList = msgRepository.findByPartIdAndPoolOrderBySendTimeAsc(part.getId(), 1);
                    for (LiveMsg liveMsg : msgList) {
                        StringBuilder builder = new StringBuilder();
                        builder.append(part.getPage()).append('#').append(format.format(new Date(liveMsg.getSendTime()))).append("  ").append(liveMsg.getContext()).append('\n');
                        //发送限制为1000
                        if(context.length()+builder.length()>1000){
                            BiliReply reply = new BiliReply();
                            reply.setType("1");
                            reply.setOid(history.getAvId());
                            reply.setAction("1");
                            reply.setMessage(context.toString());
                            replies.add(reply);
                            //重置
                            context = new StringBuilder();
                            context.append("sc和上舰列表,网页可跳转\n");
                        }
                        context.append(builder);
                    }
                }
                if(context.length()>20){
                    BiliReply reply = new BiliReply();
                    reply.setType("1");
                    reply.setOid(history.getAvId());
                    reply.setAction("1");
                    reply.setMessage(context.toString());
                    replies.add(reply);
                }
                try {
                    for (int i = 0; i < replies.size(); i++) {
                        BiliReply reply = replies.get(i);
                        BiliReplyResponse replyResponse = BiliApi.sendVideoReply(user,reply);
                        if(replyResponse.getCode() == 0){
                            log.info("av{}发送评论成功：{}",reply.getOid(),reply.getMessage());
                            history.setSendReply(true);
                            history = historyRepository.save(history);
                            //第一个评论进行置顶操作
                            if(i == 0){
                                //等待一段时间，否则无法置顶
                                Thread.sleep(2000L);
                                reply.setRpid(replyResponse.getData().getRpid());
                                reply.setAction("1");
                                BiliReplyResponse response = BiliApi.topVideoReply(user, reply);
                                if(response.getCode() != 0){
                                    log.error("av{}评论置顶失败：{}",reply.getOid(),JSON.toJSONString(response));
                                }
                                if(response.getCode() == 404){
                                    //等待一段时间，否则无法置顶
                                    Thread.sleep(2000L);
                                    BiliApi.topVideoReply(user, reply);
                                }
                            }

                            try {
                                if(StringUtils.isNotBlank(wxuid)&&StringUtils.isNotBlank(pushMsgTags)&&pushMsgTags.contains("视频评论")){
                                    Message message = new Message();
                                    message.setAppToken(wxToken);
                                    message.setContentType(Message.CONTENT_TYPE_TEXT);
                                    message.setContent(WX_MSG_FORMAT.formatted(room.getUname(),history.getTitle(),history.getBvId(),
                                            LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy年MM月dd日HH点mm分ss秒")),
                                            reply.getMessage(),"发送成功",user.getUname()+""));
                                    message.setUid(wxuid);
                                    WxPusher.send(message);
                                }
                            }catch (Exception ignored){

                            }
                        }
                        //等待一段时间在发送
                        Thread.sleep(5000L);
                    }
                }catch (Exception e){
                    log.error("发送sc评论失败：{}", JSON.toJSONString(replies),e);
                    try {
                        if(StringUtils.isNotBlank(wxuid)&&StringUtils.isNotBlank(pushMsgTags)&&pushMsgTags.contains("视频评论")){
                            Message message = new Message();
                            message.setAppToken(wxToken);
                            message.setContentType(Message.CONTENT_TYPE_TEXT);
                            message.setContent(WX_MSG_FORMAT.formatted(room.getUname(),history.getTitle(),history.getBvId(),
                                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy年MM月dd日HH点mm分ss秒")),
                                    JSON.toJSONString(replies),"发送失败",e.getMessage()));
                            message.setUid(wxuid);
                            WxPusher.send(message);
                        }
                    }catch (Exception ignored){

                    }
                }
            }
            partList.addAll(parts);
        }
        if (CollectionUtils.isEmpty(partList)) {
            return;
        }
        List<LiveMsg> msgAllList = new ArrayList<>();
        for (RecordHistoryPart part : partList) {
            List<LiveMsg> msgList = msgRepository.findByPartIdAndCode(part.getId(), -1);
            if (CollectionUtils.isEmpty(msgList)) {
                continue;
            }
            msgAllList.addAll(msgList);

        }
        if (msgAllList.isEmpty()) {
            return;
        }

        List<BiliBiliUser> allUser = userRepository.findByLoginIsTrueAndEnableIsTrue();
        if (CollectionUtils.isEmpty(allUser)) {
            return;
        }
        try {
            boolean tryLock = lock.tryLock();
            if (!tryLock) {
                log.error("弹幕发获取锁失败！！！！");
                return;
            }
            //高优先级弹幕，如sc,舰长，只能由视频发布账号发送
            List<LiveMsg> highLevelMsg = msgAllList.stream().filter(liveMsg -> liveMsg.getPool() == 1).sorted((m1, m2) -> (int)(m1.getSendTime() - m2.getSendTime())).toList();
            log.info("即将开始高级弹幕发送操作，剩余待发送弹幕{}条。", highLevelMsg.size());
            for (LiveMsg msg : highLevelMsg) {
                Long partId = msg.getPartId();
                Optional<RecordHistoryPart> partOptional = partRepository.findById(partId);
                if (partOptional.isPresent()) {
                    RecordHistoryPart part = partOptional.get();
                    String roomId = part.getRoomId();
                    RecordRoom room = roomRepository.findByRoomId(roomId);
                    if (room != null) {
                        String wxuid = room.getWxuid();
                        String pushMsgTags = room.getPushMsgTags();
                        Long uploadUserId = room.getUploadUserId();
                        Optional<BiliBiliUser> userOptional = userRepository.findById(uploadUserId);
                        if (userOptional.isPresent()) {
                            BiliBiliUser user = userOptional.get();
                            if (!(user.isLogin() && user.isEnable())) {
                                continue;
                            }
                            int code = liveMsgService.sendMsg(user, msg);
                            if (code != 0) {
                                log.error("{}用户，发送失败，错误代码{}，弹幕内容为。==>{}", user.getUname(), code, msg.getContext());
                                try {
                                    if(StringUtils.isNotBlank(wxuid)&&StringUtils.isNotBlank(pushMsgTags)&&pushMsgTags.contains("高级弹幕")){
                                        Message message = new Message();
                                        message.setAppToken(wxToken);
                                        message.setContentType(Message.CONTENT_TYPE_TEXT);
                                        message.setContent(WX_MSG_FORMAT.formatted(room.getUname(),part.getTitle(),msg.getBvid(),
                                                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy年MM月dd日HH点mm分ss秒")),
                                                msg.getContext(),"发送失败",user.getUname()+"-->code: "+code));
                                        message.setUid(wxuid);
                                        WxPusher.send(message);
                                    }
                                }catch (Exception ignored){

                                }
                            }
                            try {
                                if (code == 36703) {
                                    Thread.sleep(120 * 1000L);
                                } else if (code == 0) {
                                    Thread.sleep(25 * 1000L);
                                }
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            continue;
                        }
                    }
                }
                msg.setCode(0);
                msgRepository.save(msg);
            }
            msgAllList = msgAllList.stream().filter(liveMsg -> liveMsg.getPool() == 0).sorted((m1, m2) -> (int) (m1.getSendTime() - m2.getSendTime())).collect(Collectors.toList());
            BlockingQueue<LiveMsg> msgQueue = new ArrayBlockingQueue<>(msgAllList.size());
            msgQueue.addAll(msgAllList);
            AtomicInteger count = new AtomicInteger(0);
            log.info("即将开始普通弹幕发送操作，剩余待发送弹幕{}条。", msgQueue.size());
            allUser.stream().parallel().forEach(user -> {
                while (msgQueue.size() > 0) {
                    if (System.currentTimeMillis() - startTime > 2 * 3600 * 1000) {
                        log.error("弹幕发送超时，重新启动");
                        return;
                    }
                    LiveMsg msg = null;
                    try {
                        msg = msgQueue.poll(10, TimeUnit.MILLISECONDS);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    if (msg == null) {
                        return;
                    }
                    count.incrementAndGet();
                    user = userRepository.findByUid(user.getUid());
                    if (!(user.isLogin() && user.isEnable())) {
                        log.error("弹幕发送：有用户状态为未登录或未启用状态，退出任务。");
                        return;
                    }
                    int code = liveMsgService.sendMsg(user, msg);
                    if (code != 0 && code != 36703 && code != 36714) {
                        user = userRepository.findByUid(user.getUid());
                        code = liveMsgService.sendMsg(user, msg);
                        if(code != 0 && code != 36703 && code != 36714){
                            log.error("{}用户，发送失败，错误代码{}，一共发送{}条弹幕。", user.getUname(), code, count.get());
                            user.setEnable(false);
                            user = userRepository.save(user);
                        }
                        return;
                    } else if (code == 36703) {
                        log.error("{}用户，发送失败，错误代码{}，一共发送{}条弹幕。", user.getUname(), code, count.get());
                    }
                    try {
                        if (code == 36703) {
                            Thread.sleep(120 * 1000L);
                        } else if (code == 0) {
                            Thread.sleep(25 * 1000L);
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            });
        } finally {
            lock.unlock();
        }

    }
}
