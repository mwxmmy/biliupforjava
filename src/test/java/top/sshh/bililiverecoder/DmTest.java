package top.sshh.bililiverecoder;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Base64;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import top.sshh.bililiverecoder.entity.BiliBiliUser;
import top.sshh.bililiverecoder.entity.LiveMsg;
import top.sshh.bililiverecoder.entity.RecordHistoryPart;
import top.sshh.bililiverecoder.repo.BiliUserRepository;
import top.sshh.bililiverecoder.repo.LiveMsgRepository;
import top.sshh.bililiverecoder.repo.RecordHistoryPartRepository;
import top.sshh.bililiverecoder.service.impl.BiliBiliUserService;
import top.sshh.bililiverecoder.service.impl.LiveMsgService;
import top.sshh.bililiverecoder.util.BiliApi;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Slf4j
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class DmTest {

    @Autowired
    private RecordHistoryPartRepository partRepository;

    @Autowired
    private LiveMsgService liveMsgService;

    @Autowired
    private LiveMsgRepository msgRepository;

    @Autowired
    private BiliUserRepository biliUserRepository;
    @Autowired
    private BiliBiliUserService biliUserService;

    // @Test
    public void sendDm() {
        BiliBiliUser biliUser = biliUserRepository.findByUid(10043269L);
        List<LiveMsg> msgList = msgRepository.findByPartIdAndCode(3428L, -1);
        liveMsgService.sendMsg(biliUser,msgList.get(0));
    }

    // @Test
    public void refreshToken(){
        BiliBiliUser biliUser = biliUserRepository.findByUid(10043269L);
        boolean refreshToken = biliUserService.refreshToken(biliUser);
        System.out.println(refreshToken);
    }

    // @Test
    public void uploadCover() throws IOException {
        BiliBiliUser biliUser = biliUserRepository.findByUid(10043269L);
        File file = new File("D:/tmp/1.jpg");
        byte [] bytes = new byte[(int)file.length()];
        new FileInputStream(file).read(bytes);
        String uploadCover = BiliApi.uploadCover(biliUser,file.getName(), bytes);
        System.out.println(uploadCover);
    }
}
