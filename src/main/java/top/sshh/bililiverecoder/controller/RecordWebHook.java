package top.sshh.bililiverecoder.controller;

import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
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

    @GetMapping
    public String processing() {
        return "这里是录播姬推送的接口地址，把当前地址复制到录播姬WebHookV2里即可(前提是录播姬网络环境也可访问)";
    }
}
