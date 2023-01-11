package top.sshh.bililiverecoder;

import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import top.sshh.bililiverecoder.entity.BiliBiliUser;
import top.sshh.bililiverecoder.entity.LiveMsg;
import top.sshh.bililiverecoder.entity.RecordHistoryPart;
import top.sshh.bililiverecoder.entity.data.BiliReply;
import top.sshh.bililiverecoder.entity.data.BiliReplyResponse;
import top.sshh.bililiverecoder.repo.BiliUserRepository;
import top.sshh.bililiverecoder.repo.LiveMsgRepository;
import top.sshh.bililiverecoder.repo.RecordHistoryPartRepository;
import top.sshh.bililiverecoder.service.impl.BiliBiliUserService;
import top.sshh.bililiverecoder.service.impl.JdbcService;
import top.sshh.bililiverecoder.service.impl.LiveMsgService;
import top.sshh.bililiverecoder.util.BiliApi;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

@Slf4j
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class DmTest {

    @Autowired
    private RecordHistoryPartRepository partRepository;

    @Autowired
    private LiveMsgService liveMsgService;

    @Autowired
    private JdbcService jdbcService;

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

    // @Test
    public void replyTest(){
        BiliBiliUser biliUser = biliUserRepository.findByUid(10043269L);
        BiliReply reply = new BiliReply();
        reply.setType("1");
        reply.setOid("946830168");
        long time = 100000;
        Date date = new Date(time);
        DateFormat format = new SimpleDateFormat("mm:ss");
        format.setTimeZone(TimeZone.getTimeZone("UTC"));
        String timeStr = format.format(date);
        String timeStr2 = format.format(new Date(7350000));
        reply.setMessage("评论测试\n2#1:20\n"+timeStr+"\n"+timeStr2);
        BiliReplyResponse replyResponse = BiliApi.sendVideoReply(biliUser,reply);
        log.info("调用发送评论接口返回：{}", JSON.toJSONString(replyResponse));
        if(replyResponse.getCode() == 0){
            reply.setRpid(replyResponse.getData().getRpid());
            reply.setAction("1");
            replyResponse = BiliApi.topVideoReply(biliUser,reply);
            log.info("调用置顶评论接口返回：{}", JSON.toJSONString(replyResponse));
        }
    }

    // @Test
    public void ascTest(){
        List<RecordHistoryPart> parts = partRepository.findByHistoryIdOrderByStartTimeAsc(3529L);
        for (RecordHistoryPart part : parts) {
            log.info(JSON.toJSONString(part));
        }
    }

    // @Test
    public void preUploadTest(){
        BiliBiliUser biliUser = new BiliBiliUser();
        biliUser.setCookies("bili_jct=a951dd04c28cffc2afabeb61cbec4972;DedeUserID=10043269;gourl=http%3A%2F%2Fwww.bilibili.com;Expires=15551000;DedeUserID__ckMd5=7cc21642fb0885c3;SESSDATA=d4f40ee7%2C1688531766%2Cfc874%2A11;");
        Map<String, String> params = new TreeMap<>();
        params.put("r","upos");
        params.put("profile","ugcupos/bup");
        params.put("name","1.flv");
        params.put("size","1213123");
        String preUpload = BiliApi.preUpload(biliUser, params);
        System.out.println(preUpload);
    }
    // @Test
    public void saveDm(){
        List<LiveMsg> liveMsgs = new ArrayList<>();
        LiveMsg msg = new LiveMsg();
        msg.setPartId(1L);
        msg.setBvid("B1321");
        msg.setCid(1L);
        msg.setSendTime(1L);
        msg.setFontsize(1);
        msg.setMode(1);
        msg.setPool(0);
        msg.setColor(12);
        msg.setContext("text");
        liveMsgs.add(msg);
        jdbcService.saveLiveMsgList(liveMsgs);
    }
}
