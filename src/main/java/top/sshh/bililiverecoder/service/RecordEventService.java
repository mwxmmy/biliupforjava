package top.sshh.bililiverecoder.service;


import top.sshh.bililiverecoder.entity.RecordEventDTO;

public interface RecordEventService {

    void processing(RecordEventDTO event);
}
