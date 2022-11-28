package top.sshh.bililiverecoder.controller;

import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import top.sshh.bililiverecoder.entity.RecordEventDTO;
import top.sshh.bililiverecoder.service.RecordEventFactory;

@Slf4j
@RestController
@RequestMapping("/recordWebHook")
public class RecordWebHook {

    @Autowired
    private RecordEventFactory recordEventFactory;

    @PostMapping
    public void processing(@RequestBody RecordEventDTO recordEvent) {
        log.info("收到录播姬的推送信息==> {}", JSON.toJSONString(recordEvent));
        recordEventFactory.processing(recordEvent);
    }
}
