package top.sshh.bililiverecoder.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import top.sshh.bililiverecoder.entity.RecordEventDTO;
import top.sshh.bililiverecoder.repo.*;
import top.sshh.bililiverecoder.service.RecordEventService;

@Slf4j
@Component
public class RecordRoomChangeService implements RecordEventService {

    @Autowired
    private BiliUserRepository biliUserRepository;

    @Autowired
    private RecordRoomRepository roomRepository;

    @Autowired
    private RecordHistoryRepository historyRepository;

    @Autowired
    private RecordHistoryPartRepository historyPartRepository;

    @Autowired
    private LiveMsgRepository liveMsgRepository;


    @Override
    public void processing(RecordEventDTO event) {

    }
}
