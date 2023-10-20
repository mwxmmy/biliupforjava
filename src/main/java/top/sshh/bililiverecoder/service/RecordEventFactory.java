package top.sshh.bililiverecoder.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import top.sshh.bililiverecoder.entity.*;
import top.sshh.bililiverecoder.service.impl.*;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
public class RecordEventFactory {

    @Autowired
    private RecordEventRecordStartedService recordEventRecordStartedService;
    @Autowired
    private RecordEventRecordEndService recordEventRecordEndService;
    @Autowired
    private RecordRoomChangeService recordRoomChangeService;
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

    private final Map<String, BlrecRoomInfo> blrecRoomInfoMap = new HashMap<>();
    private final Map<String, BlrecUserInfo> blrecUserInfoMap = new HashMap<>();


    public RecordEventService getEventService(String eventType) {
        return switch (eventType) {
            case RecordEventType.SessionStarted, RecordEventType.RecordingStartedEvent ->
                    recordEventRecordStartedService;
            case RecordEventType.SessionEnded, RecordEventType.RecordingFinishedEvent, RecordEventType.RecordingCancelledEvent ->
                    recordEventRecordEndService;
            case RecordEventType.RoomChangeEvent -> recordRoomChangeService;
            case RecordEventType.StreamStarted, RecordEventType.LiveBeganEvent -> recordEventStreamStartService;
            case RecordEventType.StreamEnded, RecordEventType.LiveEndedEvent -> recordEventStreamEndService;
            case RecordEventType.FileOpening, RecordEventType.VideoFileCreatedEvent -> recordEventFileOpenService;
            case RecordEventType.FileClosed, RecordEventType.VideoFileCompletedEvent -> recordEventFileClosedService;
            default -> recordEventEmptyService;
        };
    }

    @Async
    public void processing(RecordEventDTO eventDTO) {
        if (StringUtils.isBlank(eventDTO.getEventType())) {
            if (eventDTO.getData() != null) {
                eventDTO.setEventType(eventDTO.getType());
                RecordEventData eventData = new RecordEventData();
                eventDTO.setEventData(eventData);
                BlrecRoomInfo roomInfo = eventDTO.getData().getRoomInfo();
                BlrecUserInfo userInfo = eventDTO.getData().getUserInfo();
                eventDTO.setEventId(eventDTO.getId());
                eventData.setSessionId("blrec");
                eventData.setRecording(false);
                String path = eventDTO.getData().getPath();
                if (StringUtils.isNotBlank(path)) {
                    path = path.replace("\\", "/");
                    eventData.setRelativePath(path);
                }
                String roomId = eventDTO.getData().getRoomId();
                if (StringUtils.isNotBlank(roomId)) {
                    eventData.setRoomId(roomId);
                }
                if (roomInfo != null) {
                    blrecRoomInfoMap.put(roomInfo.getRoomId(), roomInfo);
                } else {
                    roomInfo = blrecRoomInfoMap.get(roomId);
                }
                if (userInfo != null) {
                    blrecUserInfoMap.put(userInfo.getUid(), userInfo);
                } else if (roomInfo != null) {
                    userInfo = blrecUserInfoMap.get(roomInfo.getUid());
                }
                if (roomInfo != null) {
                    eventData.setRoomId(roomInfo.getRoomId());
                    eventData.setShortId(roomInfo.getSortRoomId());
                    eventData.setTitle(roomInfo.getTitle());
                    eventData.setAreaNameParent(roomInfo.getParentAreaName());
                    eventData.setAreaNameChild(roomInfo.getAreaName());
                    eventData.setRecording(roomInfo.getLiveStatus() == 1);
                }
                if (userInfo != null) {
                    eventData.setName(userInfo.getName());
                }

            }
        }
        String eventType = eventDTO.getEventType();
        if (StringUtils.isBlank(eventType)) {
            log.error("事件类型为空");
            return;
        }
        RecordEventService eventService = this.getEventService(eventType);
        eventService.processing(eventDTO);
    }
}
