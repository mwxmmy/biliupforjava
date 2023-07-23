package top.sshh.bililiverecoder.controller;


import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import top.sshh.bililiverecoder.entity.*;
import top.sshh.bililiverecoder.repo.LiveMsgRepository;
import top.sshh.bililiverecoder.repo.RecordHistoryPartRepository;
import top.sshh.bililiverecoder.repo.RecordHistoryRepository;
import top.sshh.bililiverecoder.repo.RecordRoomRepository;
import top.sshh.bililiverecoder.service.impl.LiveMsgService;
import top.sshh.bililiverecoder.service.impl.RecordBiliPublishService;

import java.io.File;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@RestController
@RequestMapping("/history")
public class HistoryController {


    @Value("${record.work-path}")
    private String workPath;
    @Autowired
    private RecordHistoryRepository historyRepository;
    @Autowired
    private RecordRoomRepository roomRepository;
    @Autowired
    private RecordHistoryPartRepository partRepository;
    @Autowired
    private RecordBiliPublishService publishService;
    @Autowired
    private LiveMsgRepository msgRepository;
    @Autowired
    private LiveMsgService msgService;
    @PersistenceContext
    private EntityManager entityManager;

    @PostMapping("/list")
    public Map<String,Object> list(@RequestBody RecordHistoryDTO request) {
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        // 指定结果视图
        CriteriaQuery<RecordHistory> criteriaQuery = criteriaBuilder.createQuery(RecordHistory.class);
        // 查询基础表
        Root<RecordHistory> root = criteriaQuery.from(RecordHistory.class);
        criteriaQuery.select(root);
        //Predicate 过滤条件 构建where字句可能的各种条件
        //这里用List存放多种查询条件,实现动态查询
        List<Predicate> predicatesList = new ArrayList<>();
        if (StringUtils.isNotBlank(request.getRoomId())) {
            predicatesList.add(criteriaBuilder.and(criteriaBuilder.equal(root.get("roomId"), request.getRoomId())));
        }
        if (StringUtils.isNotBlank(request.getBvId())) {
            predicatesList.add(criteriaBuilder.and(criteriaBuilder.like(root.get("bvId"), "%" + request.getBvId() + "%")));
        }
        if (StringUtils.isNotBlank(request.getTitle())) {
            predicatesList.add(criteriaBuilder.and(criteriaBuilder.like(root.get("title"), "%" + request.getTitle() + "%")));
        }

        if (request.getRecording() != null) {
            predicatesList.add(criteriaBuilder.and(criteriaBuilder.equal(root.get("recording"), request.getRecording())));
        }
        if (request.getUpload() != null) {
            predicatesList.add(criteriaBuilder.and(criteriaBuilder.equal(root.get("upload"), request.getUpload())));
        }
        if (request.getPublish() != null) {
            predicatesList.add(criteriaBuilder.and(criteriaBuilder.equal(root.get("publish"), request.getPublish())));
        }

        if (request.getFrom() != null && request.getTo() != null) {
            predicatesList.add(criteriaBuilder.and(criteriaBuilder.between(root.get("endTime"), request.getFrom(), request.getTo())));
        }
        //where()拼接查询条件
        if (predicatesList.size() > 0) {
            criteriaQuery.where(predicatesList.toArray(new Predicate[predicatesList.size()]));
        }
        criteriaQuery.orderBy(criteriaBuilder.desc(root.get("endTime")));
        TypedQuery<RecordHistory> typedQuery = entityManager.createQuery(criteriaQuery);
        int total = typedQuery.getResultList().size();
        typedQuery.setFirstResult((request.getCurrent()-1)*request.getPageSize());
        typedQuery.setMaxResults(request.getPageSize());
        List<RecordHistory> list = typedQuery.getResultList();
        Map<String,String> roomCache = new HashMap<>();
        List<Runnable> runnables = new ArrayList<>();
        Iterable<RecordRoom> iterable = roomRepository.findAll();
        for (RecordRoom recordRoom : iterable) {
            roomCache.put(recordRoom.getRoomId(),recordRoom.getUname());
        }
        for (RecordHistory history : list) {
            history.setRoomName(roomCache.get(history.getRoomId()));
            Runnable run;
            run = () -> history.setPartCount(partRepository.countByHistoryId(history.getId()));
            runnables.add(run);
            run = () -> history.setPartDuration(partRepository.sumHistoryDurationByHistoryId(history.getId()));
            runnables.add(run);
            run = () -> history.setUploadPartCount(partRepository.countByHistoryIdAndFileNameNotNull(history.getId()));
            runnables.add(run);
            run = () -> history.setRecordPartCount(partRepository.countByHistoryIdAndRecordingIsTrue(history.getId()));
            runnables.add(run);
            run = () -> history.setPartCount(partRepository.countByHistoryId(history.getId()));
            runnables.add(run);
            if (StringUtils.isNotBlank(history.getBvId())) {
                run = () -> history.setMsgCount(msgRepository.countByBvid(history.getBvId()));
                runnables.add(run);
                run = () -> history.setSuccessMsgCount(msgRepository.countByBvidAndCode(history.getBvId(), 0));
                runnables.add(run);
            }
        }
        runnables.stream().parallel().forEach(Runnable::run);
        Map<String,Object> result = new HashMap<>();
        result.put("data",list);
        result.put("total",total);
        return result;
    }


    @PostMapping("/update")
    public Map<String, String> update(@RequestBody RecordHistory history) {
        Optional<RecordHistory> historyOptional = historyRepository.findById(history.getId());
        Map<String, String> result = new HashMap<>();
        if (historyOptional.isPresent()) {
            RecordHistory dbHistory = historyOptional.get();
            dbHistory.setRecording(history.isRecording());
            dbHistory.setUpload(history.isUpload());
            dbHistory.setUpdateTime(LocalDateTime.now());
            historyRepository.save(dbHistory);
            result.put("type", "info");
            result.put("msg", "更新成功");
        }
        return result;
    }

    @GetMapping("/delete/{id}")
    public Map<String, String> delete(@PathVariable("id") Long id) {
        Map<String, String> result = new HashMap<>();
        if (id == null) {
            result.put("type", "info");
            result.put("msg", "请输入id");
            return result;
        }
        Optional<RecordHistory> historyOptional = historyRepository.findById(id);
        if (historyOptional.isPresent()) {
            RecordHistory history = historyOptional.get();
            List<LiveMsg> liveMsgs = msgRepository.queryByBvid(history.getBvId());
            msgRepository.deleteAll(liveMsgs);
            List<RecordHistoryPart> partList = partRepository.findByHistoryIdOrderByStartTimeAsc(history.getId());
            for (RecordHistoryPart part : partList) {
                String filePath = part.getFilePath();
                if(! filePath.startsWith(workPath)){
                    part.setFileDelete(true);
                    part = partRepository.save(part);
                    continue;
                }
                String startDirPath = filePath.substring(0, filePath.lastIndexOf('/') + 1);
                String fileName = filePath.substring(filePath.lastIndexOf("/") + 1, filePath.lastIndexOf("."));
                File startDir = new File(startDirPath);
                File[] files = startDir.listFiles((file, s) -> s.startsWith(fileName));
                if (files != null) {
                    for (File file : files) {
                        file.delete();
                    }
                }
                part.setFileDelete(true);
                partRepository.save(part);
            }
            partRepository.deleteAll(partList);
            historyRepository.delete(history);
            result.put("type", "success");
            result.put("msg", "录制历史删除成功");
            return result;
        } else {
            result.put("type", "warning");
            result.put("msg", "录制历史不存在");
            return result;
        }
    }

    @GetMapping("/deleteMsg/{id}")
    public Map<String, String> deleteMsg(@PathVariable("id") Long id) {
        Map<String, String> result = new HashMap<>();
        if (id == null) {
            result.put("type", "info");
            result.put("msg", "请输入id");
            return result;
        }
        Optional<RecordHistory> historyOptional = historyRepository.findById(id);
        if (historyOptional.isPresent()) {
            RecordHistory history = historyOptional.get();
            List<LiveMsg> liveMsgs = msgRepository.queryByBvid(history.getBvId());
            msgRepository.deleteAll(liveMsgs);
            result.put("type", "success");
            result.put("msg", "弹幕删除成功");
            return result;
        } else {
            result.put("type", "warning");
            result.put("msg", "录制历史不存在");
            return result;
        }
    }

    @GetMapping("/reloadMsg/{id}")
    public Map<String, String> reloadMsg(@PathVariable("id") Long id) {
        Map<String, String> result = new HashMap<>();
        if (id == null) {
            result.put("type", "info");
            result.put("msg", "请输入id");
            return result;
        }
        Optional<RecordHistory> historyOptional = historyRepository.findById(id);
        if (historyOptional.isPresent()) {
            RecordHistory history = historyOptional.get();
            List<RecordHistoryPart> parts = partRepository.findByHistoryIdOrderByStartTimeAsc(history.getId());
            for (RecordHistoryPart part : parts) {
                String filePath = part.getFilePath();
                filePath = filePath.replaceAll(".flv", ".xml");
                File file = new File(filePath);
                if (file.exists()) {
                    List<LiveMsg> liveMsgs = msgRepository.queryByCid(part.getCid());
                    msgRepository.deleteAll(liveMsgs);
                    msgService.processing(part);
                }
            }
            result.put("type", "success");
            result.put("msg", "弹幕重新加载成功");
            return result;
        } else {
            result.put("type", "warning");
            result.put("msg", "录制历史不存在");
            return result;
        }
    }

    @GetMapping("/updatePartStatus/{id}")
    public Map<String, String> updatePartStatus(@PathVariable("id") Long id) {
        Map<String, String> result = new HashMap<>();
        if (id == null) {
            result.put("type", "info");
            result.put("msg", "请输入id");
            return result;
        }
        Optional<RecordHistory> historyOptional = historyRepository.findById(id);
        if (historyOptional.isPresent()) {
            RecordHistory history = historyOptional.get();
            List<RecordHistoryPart> partList = partRepository.findByHistoryIdOrderByStartTimeAsc(history.getId());
            for (RecordHistoryPart part : partList) {
                part.setRecording(false);
                partRepository.save(part);
            }
            history.setRecording(false);
            historyRepository.save(history);
            result.put("type", "success");
            result.put("msg", "状态更新成功");
            return result;
        } else {
            result.put("type", "warning");
            result.put("msg", "录制历史不存在");
            return result;
        }
    }

    @GetMapping("/updatePublishStatus/{id}")
    public Map<String, String> updatePublishStatus(@PathVariable("id") Long id) {
        Map<String, String> result = new HashMap<>();
        if (id == null) {
            result.put("type", "info");
            result.put("msg", "请输入id");
            return result;
        }
        Optional<RecordHistory> historyOptional = historyRepository.findById(id);
        if (historyOptional.isPresent()) {
            RecordHistory history = historyOptional.get();
            history.setStartTime(history.getStartTime().plusMinutes(1L));
            history.setPublish(false);
            history.setBvId(null);
            history.setCode(-1);
            historyRepository.save(history);
            List<RecordHistoryPart> partList = partRepository.findByHistoryIdOrderByStartTimeAsc(history.getId());
            for (RecordHistoryPart part : partList) {
                part.setUpload(false);
                partRepository.save(part);
            }
            result.put("type", "success");
            result.put("msg", "状态更新成功");
            return result;
        } else {
            result.put("type", "warning");
            result.put("msg", "录制历史不存在");
            return result;
        }
    }

    @GetMapping("/touchPublish/{id}")
    public Map<String, String> touchPublish(@PathVariable("id") Long id) {
        Map<String, String> result = new HashMap<>();
        if (id == null) {
            result.put("type", "info");
            result.put("msg", "请输入id");
            return result;
        }
        Optional<RecordHistory> historyOptional = historyRepository.findById(id);
        if (historyOptional.isPresent()) {
            RecordHistory history = historyOptional.get();
            history.setUploadRetryCount(0);
            history = historyRepository.save(history);
            publishService.asyncPublishRecordHistory(history);
            result.put("type", "success");
            result.put("msg", "触发发布事件成功");
            return result;
        } else {
            result.put("type", "warning");
            result.put("msg", "录制历史不存在");
            return result;
        }
    }

    @GetMapping("/rePublish/{id}")
    public Map<String, String> rePublish(@PathVariable("id") Long id) {
        Map<String, String> result = new HashMap<>();
        if (id == null) {
            result.put("type", "info");
            result.put("msg", "请输入id");
            return result;
        }
        Optional<RecordHistory> historyOptional = historyRepository.findById(id);
        if (historyOptional.isPresent()) {
            RecordHistory history = historyOptional.get();
            publishService.asyncRepublishRecordHistory(history);
            result.put("type", "success");
            result.put("msg", "触发转码修复事件成功");
            return result;
        } else {
            result.put("type", "warning");
            result.put("msg", "录制历史不存在");
            return result;
        }
    }
}
