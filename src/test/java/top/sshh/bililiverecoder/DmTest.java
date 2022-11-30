//package top.sshh.bililiverecoder;
//
//import lombok.extern.slf4j.Slf4j;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.context.SpringBootTest;
//import top.sshh.bililiverecoder.entity.BiliBiliUser;
//import top.sshh.bililiverecoder.entity.LiveMsg;
//import top.sshh.bililiverecoder.repo.BiliUserRepository;
//import top.sshh.bililiverecoder.repo.LiveMsgRepository;
//import top.sshh.bililiverecoder.repo.RecordHistoryPartRepository;
//import top.sshh.bililiverecoder.service.impl.LiveMsgService;
//
//import java.util.ArrayList;
//import java.util.List;
//import java.util.concurrent.LinkedBlockingQueue;
//import java.util.concurrent.atomic.AtomicInteger;
//
//@Slf4j
//@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
//public class DmTest {
//
//    @Autowired
//    private RecordHistoryPartRepository partRepository;
//
//    @Autowired
//    private LiveMsgService liveMsgService;
//
//    @Autowired
//    private LiveMsgRepository msgRepository;
//
//    @Autowired
//    private BiliUserRepository biliUserRepository;
//
//    @Test
//    public void sendDm(){
//        Iterable<BiliBiliUser> all = biliUserRepository.findAll();
//        List<BiliBiliUser> users = new ArrayList<>();
//        all.forEach(users::add);
//        AtomicInteger count = new AtomicInteger(0);
//        List<LiveMsg> msgList = msgRepository.findByPartIdAndCode(44L,-1);
//        LinkedBlockingQueue<LiveMsg> msgLinkedList = new LinkedBlockingQueue<>(msgList);
//        users.stream().parallel().forEach(user->{
//            while (msgLinkedList.size()>0){
//                LiveMsg msg = msgLinkedList.poll();
//                count.incrementAndGet();
//                int code = liveMsgService.sendMsg(user, msg);
//                if (code != 0 && code != 36703) {
//                    log.error("{}用户，发送失败，错误代码{}，一共发送{}条弹幕。",user.getUname(),code,count.get());
//                    return;
//                }else if(code == 36703){
//                    log.error("{}用户，发送失败，错误代码{}，一共发送{}条弹幕。",user.getUname(),code,count.get());
//                }
//                try {
//                    if(code == 36703){
//                        Thread.sleep(100*1000L);
//                    }else {
//                        Thread.sleep(15*1000L);
//                    }
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
//            }
//        });
//    }
//}
