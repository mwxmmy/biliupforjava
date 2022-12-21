package top.sshh.bililiverecoder.job;

import com.alibaba.fastjson.JSON;
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
import top.sshh.bililiverecoder.service.impl.BiliBiliUserService;
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
public class RefreshTokenJob {

    @Autowired
    private BiliUserRepository userRepository;

    @Autowired
    private BiliBiliUserService userService;


    @Scheduled(fixedDelay = 3600000, initialDelay = 0)
    public void sndMsgProcess() {
        Iterable<BiliBiliUser> all = userRepository.findAll();
        for (BiliBiliUser user : all) {
            try {
                userService.refreshToken(user);
            }catch (Exception e){
                log.error("刷新token失败==>{}", JSON.toJSONString(user));
            }
        }
    }
}
