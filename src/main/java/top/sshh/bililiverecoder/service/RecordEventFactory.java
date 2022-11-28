package top.sshh.bililiverecoder.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import top.sshh.bililiverecoder.entity.RecordEventDTO;
import top.sshh.bililiverecoder.entity.RecordEventType;
import top.sshh.bililiverecoder.service.impl.*;

@Slf4j
@Component
public class RecordEventFactory {

    @Autowired
    RecordEventRecordStartedService recordEventRecordStartedService;
    @Autowired
    RecordEventStreamStartService recordEventStreamStartService;
    @Autowired
    RecordEventStreamEndService recordEventStreamEndService;
    @Autowired
    RecordEventFileOpenService recordEventFileOpenService;
    @Autowired
    RecordEventFileClosedService recordEventFileClosedService;
    @Autowired
    RecordEventEmptyService recordEventEmptyService;


    public RecordEventService getEventService(String eventType){
        switch (eventType){
            case RecordEventType.SessionStarted:return recordEventRecordStartedService;
            case RecordEventType.StreamStarted:return recordEventStreamStartService;
            case RecordEventType.StreamEnded:return recordEventStreamEndService;
            case RecordEventType.FileOpening:return recordEventFileOpenService;
            case RecordEventType.FileClosed:return recordEventFileClosedService;
            default:return recordEventEmptyService;
        }
    }

    public void processing(RecordEventDTO eventDTO){
        String eventType = eventDTO.getEventType();
        if (StringUtils.isBlank(eventType)){
            log.error("事件类型为空");
            return;
        }
        RecordEventService eventService = this.getEventService(eventType);
        eventService.processing(eventDTO);
    }
}
