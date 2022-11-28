package top.sshh.bililiverecoder;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableAsync
@SpringBootApplication
public class BiliLiveRecordApplication {

    public static void main(String[] args) {
        SpringApplication.run(BiliLiveRecordApplication.class, args);
    }

}
