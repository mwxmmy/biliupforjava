package top.sshh.bililiverecoder.job;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import top.sshh.bililiverecoder.entity.BiliBiliUser;
import top.sshh.bililiverecoder.entity.LiveMsg;
import top.sshh.bililiverecoder.entity.RecordHistory;
import top.sshh.bililiverecoder.entity.RecordHistoryPart;
import top.sshh.bililiverecoder.repo.BiliUserRepository;
import top.sshh.bililiverecoder.repo.LiveMsgRepository;
import top.sshh.bililiverecoder.repo.RecordHistoryPartRepository;
import top.sshh.bililiverecoder.repo.RecordHistoryRepository;
import top.sshh.bililiverecoder.service.impl.LiveMsgService;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

@Slf4j
@Component
public class LiveMsgSendSync {

    @Autowired
    private BiliUserRepository userRepository;

    @Autowired
    private LiveMsgRepository msgRepository;

    @Autowired
    private RecordHistoryRepository historyRepository;

    @Autowired
    private RecordHistoryPartRepository partRepository;

    @Autowired
    private LiveMsgService liveMsgService;

    private static final Lock lock = new ReentrantLock();

    @Scheduled(cron = "0 * * * * ?")
    public void sndMsgProcess() {
        log.error("发送弹幕定时任务开始");
        long startTime = System.currentTimeMillis();
        List<RecordHistory> historyList = historyRepository.findByPublishIsTrueAndCode(0);
        if (CollectionUtils.isEmpty(historyList)) {
            return;
        }
        List<RecordHistoryPart> partList = new ArrayList<>();
        for (RecordHistory history : historyList) {
            List<RecordHistoryPart> parts = partRepository.findByHistoryIdAndCidIsNotNull(history.getId());
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
            msgAllList = msgAllList.stream().sorted((m1, m2) -> (int) (m1.getSendTime() - m2.getSendTime())).collect(Collectors.toList());
            Queue<LiveMsg> msgQueue = new LinkedList<>(msgAllList);
            AtomicInteger count = new AtomicInteger(0);
            log.error("即将开始弹幕发送操作，剩余待发送弹幕{}条。", msgQueue.size());
            allUser.stream().parallel().forEach(user -> {
                while (msgQueue.size() > 0) {
                    if (System.currentTimeMillis() - startTime > 2 * 3600 * 1000) {
                        log.error("弹幕发送超时，重新启动");
                        return;
                    }
                    LiveMsg msg = msgQueue.poll();
                    count.incrementAndGet();
                    int code = liveMsgService.sendMsg(user, msg);
                    if (code != 0 && code != 36703 && code != 36714 && code != -101) {
                        log.error("{}用户，发送失败，错误代码{}，一共发送{}条弹幕。", user.getUname(), code, count.get());
                        user.setEnable(false);
                        user = userRepository.save(user);
                        return;
                    } else if (code == 36703) {
                        log.error("{}用户，发送失败，错误代码{}，一共发送{}条弹幕。", user.getUname(), code, count.get());
                    }
                    try {
                        if (code == 36703) {
                            Thread.sleep(120 * 1000L);
                        } else {
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
