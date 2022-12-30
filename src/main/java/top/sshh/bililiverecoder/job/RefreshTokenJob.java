package top.sshh.bililiverecoder.job;

import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import top.sshh.bililiverecoder.entity.BiliBiliUser;
import top.sshh.bililiverecoder.repo.BiliUserRepository;
import top.sshh.bililiverecoder.service.impl.BiliBiliUserService;

@Slf4j
@Component
public class RefreshTokenJob {

    @Autowired
    private BiliUserRepository userRepository;

    @Autowired
    private BiliBiliUserService userService;


    @Scheduled(fixedDelay = 72000000, initialDelay = 60000)
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
