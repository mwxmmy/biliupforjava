package top.sshh.bililiverecoder.controller;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import top.sshh.bililiverecoder.entity.RecordRoom;
import top.sshh.bililiverecoder.repo.RecordRoomRepository;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@RestController
@RequestMapping("/room")
public class RoomController {
    @Autowired
    private RecordRoomRepository roomRepository;



    @PostMapping
    public List<RecordRoom> list() {
        Iterator<RecordRoom> roomIterator = roomRepository.findAll().iterator();
        List<RecordRoom> list = new ArrayList<>();
        roomIterator.forEachRemaining(list::add);
        return list;
    }

    @PostMapping("/update")
    public boolean update(@RequestBody RecordRoom room) {
        Optional<RecordRoom> roomOptional = roomRepository.findById(room.getId());
        if (roomOptional.isPresent()) {
            RecordRoom dbRoom = roomOptional.get();
            dbRoom.setTid(room.getTid());
            dbRoom.setTags(room.getTags());
            dbRoom.setUpload(room.isUpload());
            dbRoom.setUploadUserId(room.getUploadUserId());
            dbRoom.setTitleTemplate(room.getTitleTemplate());
            dbRoom.setPartTitleTemplate(room.getPartTitleTemplate());
            dbRoom.setDescTemplate(room.getDescTemplate());
            dbRoom.setCopyright(room.getCopyright());
            dbRoom.setFileSizeLimit(room.getFileSizeLimit());
            dbRoom.setDurationLimit(room.getDurationLimit());
            dbRoom.setDeleteFile(room.isDeleteFile());
            roomRepository.save(dbRoom);
            return true;
        }
        return false;
    }

    @PostMapping("/add")
    public Map<String, String> add(@RequestBody RecordRoom add) {
        Map<String, String> result = new HashMap<>();
        if (StringUtils.isBlank(add.getRoomId())) {
            result.put("type", "info");
            result.put("msg", "请输入房间号");
            return result;
        }

        RecordRoom room = roomRepository.findByRoomId(add.getRoomId());
        if (room != null) {
            result.put("type", "warning");
            result.put("msg", "房间号已存在");
            return result;
        } else {
            room = new RecordRoom();
            room.setRoomId(add.getRoomId());
            roomRepository.save(room);
            result.put("type", "success");
            result.put("msg", "添加成功");
            return result;
        }
    }

    @GetMapping("/delete/{roomId}")
    public Map<String, String> add(@PathVariable("roomId") String roomId) {
        Map<String, String> result = new HashMap<>();
        if (StringUtils.isBlank(roomId)) {
            result.put("type", "info");
            result.put("msg", "请输入房间号");
            return result;
        }

        RecordRoom room = roomRepository.findByRoomId(roomId);
        if (room != null) {
            roomRepository.delete(room);
            result.put("type", "success");
            result.put("msg", "房间删除成功");
            return result;
        } else {
            result.put("type", "warning");
            result.put("msg", "房间不存在");
            return result;
        }
    }

    @GetMapping("/verification")
    public String verification(String template) {
        template = template.replace("${uname}", "主播名称")
                .replace("${title}", "直播标题")
                .replace("${roomId}", "房间号");
        if (template.contains("${")) {
            String date = template.substring(template.indexOf("${"), template.indexOf("}") + 1);
            String format = LocalDateTime.now().format(DateTimeFormatter.ofPattern(date.substring(2, date.length() - 1)));
            template = template.replace(date, format);
        }


        return template;
    }
}
