package top.sshh.bililiverecoder.job;

import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import top.sshh.bililiverecoder.entity.BiliBiliUser;
import top.sshh.bililiverecoder.repo.BiliUserRepository;
import top.sshh.bililiverecoder.service.impl.BiliBiliUserService;

import java.time.LocalDateTime;

@Slf4j
@Component
public class RefreshTokenJob {

    @Autowired
    private BiliUserRepository userRepository;

    @Autowired
    private BiliBiliUserService userService;


    //两天更新一次
    @Scheduled(fixedDelay = 172800000, initialDelay = 60000)
    public void sndMsgProcess() {
        LocalDateTime now = LocalDateTime.now().minusHours(1);
        Iterable<BiliBiliUser> all = userRepository.findAll();
        for (BiliBiliUser user : all) {
            LocalDateTime updateTime = user.getUpdateTime();
            if(updateTime.isAfter(now)){
                log.error("刷新token，距离上次更新不超过一小时，跳过==>{}", user.getUname());
                continue;
            }
            try {
                userService.refreshToken(user);
            }catch (Exception e){
                log.error("刷新token失败==>{}", JSON.toJSONString(user));
                e.printStackTrace();
            }
        }
    }
}
