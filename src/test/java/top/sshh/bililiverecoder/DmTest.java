//package top.sshh.bililiverecoder;
//
//import org.junit.jupiter.api.Test;
//import org.springframework.boot.test.context.SpringBootTest;
//import top.sshh.bililiverecoder.entity.LiveMsg;
//import top.sshh.bililiverecoder.entity.data.BiliDmResponse;
//import top.sshh.bililiverecoder.util.BiliApi;
//
//@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
//public class DmTest {
//
//    @Test
//    public void sendDm(){
//        LiveMsg msg = new LiveMsg();
//        msg.setBvid("BV1A8411j77H");
//        msg.setCid(906282287L);
//        msg.setContext("c宝我来了！！");
//        msg.setSendTime(5000L);
//        BiliDmResponse dmResponse = BiliApi.sendVideoDm("80b0f33b59d3700832d2b7749268b6b1", msg);
//        System.out.println(dmResponse);
//    }
//}
