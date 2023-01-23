package top.sshh.bililiverecoder.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import top.sshh.bililiverecoder.entity.RecordEventDTO;
import top.sshh.bililiverecoder.entity.RecordEventType;
import top.sshh.bililiverecoder.service.impl.*;

@Slf4j
@Component
public class RecordEventFactory {

    @Autowired
    private RecordEventRecordStartedService recordEventRecordStartedService;
    @Autowired
    private RecordEventRecordEndService recordEventRecordEndService;
    @Autowired
    private RecordEventStreamStartService recordEventStreamStartService;
    @Autowired
    private RecordEventStreamEndService recordEventStreamEndService;
    @Autowired
    private RecordEventFileOpenService recordEventFileOpenService;
    @Autowired
    private RecordEventFileClosedService recordEventFileClosedService;
    @Autowired
    private RecordEventEmptyService recordEventEmptyService;


    public RecordEventService getEventService(String eventType){
        return switch (eventType) {
            case RecordEventType.SessionStarted -> recordEventRecordStartedService;
            case RecordEventType.SessionEnded -> recordEventRecordEndService;
            case RecordEventType.StreamStarted -> recordEventStreamStartService;
            case RecordEventType.StreamEnded -> recordEventStreamEndService;
            case RecordEventType.FileOpening -> recordEventFileOpenService;
            case RecordEventType.FileClosed -> recordEventFileClosedService;
            default -> recordEventEmptyService;
        };
    }

    @Async
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
