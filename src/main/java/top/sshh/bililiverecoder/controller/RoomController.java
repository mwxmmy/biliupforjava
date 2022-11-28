package top.sshh.bililiverecoder.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import top.sshh.bililiverecoder.entity.RecordRoom;
import top.sshh.bililiverecoder.repo.RecordRoomRepository;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

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
            dbRoom.setDeleteFile(room.isDeleteFile());
            roomRepository.save(dbRoom);
            return true;
        }
        return false;
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
