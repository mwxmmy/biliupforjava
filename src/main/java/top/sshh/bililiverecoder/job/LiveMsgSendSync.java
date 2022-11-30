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
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

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

    @Scheduled(fixedDelay = 1000, initialDelay = 0)
    public void sndMsgProcess() {
        log.error("发送弹幕定时任务开始");
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
        for (RecordHistoryPart part : partList) {
            AtomicInteger count = new AtomicInteger(0);
            List<LiveMsg> msgList = msgRepository.findByPartIdAndCode(part.getId(), -1);
            if (CollectionUtils.isEmpty(msgList)) {
                continue;
            }

            List<BiliBiliUser> allUser = userRepository.findByLoginIsTrueAndEnableIsTrue();
            if (CollectionUtils.isEmpty(allUser)) {
                return;
            }
            LinkedBlockingQueue<LiveMsg> msgLinkedList = new LinkedBlockingQueue<>(msgList);
            allUser.stream().parallel().forEach(user -> {
                while (msgLinkedList.size() > 0) {
                    LiveMsg msg = msgLinkedList.poll();
                    count.incrementAndGet();
                    int code = liveMsgService.sendMsg(user, msg);
                    if (code != 0 && code != 36703) {
                        log.error("{}用户，发送失败，错误代码{}，一共发送{}条弹幕。", user.getUname(), code, count.get());
                        return;
                    } else if (code == 36703) {
                        user.setEnable(false);
                        userRepository.save(user);
                        log.error("{}用户，发送失败，错误代码{}，一共发送{}条弹幕。", user.getUname(), code, count.get());
                    }
                    try {
                        if (code == 36703) {
                            Thread.sleep(100 * 1000L);
                        } else {
                            Thread.sleep(15 * 1000L);
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            });
        }

    }
}
