package top.sshh.bililiverecoder;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import top.sshh.bililiverecoder.entity.RecordHistoryPart;
import top.sshh.bililiverecoder.repo.BiliUserRepository;
import top.sshh.bililiverecoder.repo.LiveMsgRepository;
import top.sshh.bililiverecoder.repo.RecordHistoryPartRepository;
import top.sshh.bililiverecoder.service.impl.LiveMsgService;

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

    @Test
    public void sendDm() {
        Optional<RecordHistoryPart> partOptional = partRepository.findById(3136L);
        if (partOptional.isPresent()) {
            RecordHistoryPart part = partOptional.get();
            part.setFilePath("E:/tmp/退役之后过上慢生活-001.flv");
            liveMsgService.processing(part);
        }
    }
}
