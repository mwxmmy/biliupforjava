package top.sshh.bililiverecoder.service.impl;

import com.alibaba.fastjson.JSON;
import com.zjiecode.wxpusher.client.WxPusher;
import com.zjiecode.wxpusher.client.bean.Message;
import lombok.extern.slf4j.Slf4j;
import net.bramp.ffmpeg.FFmpeg;
import net.bramp.ffmpeg.FFmpegExecutor;
import net.bramp.ffmpeg.builder.FFmpegBuilder;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import top.sshh.bililiverecoder.entity.*;
import top.sshh.bililiverecoder.entity.data.SingleVideoDto;
import top.sshh.bililiverecoder.entity.data.VideoUploadDto;
import top.sshh.bililiverecoder.repo.BiliUserRepository;
import top.sshh.bililiverecoder.repo.LiveMsgRepository;
import top.sshh.bililiverecoder.repo.RecordHistoryPartRepository;
import top.sshh.bililiverecoder.repo.RecordRoomRepository;
import top.sshh.bililiverecoder.util.BiliApi;
import top.sshh.bililiverecoder.util.UploadEnums;
import top.sshh.bililiverecoder.util.bili.Cookie;
import top.sshh.bililiverecoder.util.bili.WebCookie;
import top.sshh.bililiverecoder.util.bili.upload.ChunkUploadRequest;
import top.sshh.bililiverecoder.util.bili.upload.CompleteUploadRequest;
import top.sshh.bililiverecoder.util.bili.upload.LineUploadRequest;
import top.sshh.bililiverecoder.util.bili.upload.PreUploadRequest;
import top.sshh.bililiverecoder.util.bili.upload.pojo.CompleteUploadBean;
import top.sshh.bililiverecoder.util.bili.upload.pojo.LineUploadBean;
import top.sshh.bililiverecoder.util.bili.upload.pojo.PreUploadBean;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Component
public class HighEnergyCutPublishService {

    public static final Map<Long, String> taskRunningMsg = new ConcurrentHashMap<>();
    private static final String WX_MSG_FORMAT = """
            结果: %s
            主播%s高能切片事件
            房间名: %s
            时间: %s
            原因: %s
            """;
    @Value("${record.wx-push-token}")
    private String wxToken;
    @Autowired
    private BiliUserRepository biliUserRepository;
    @Autowired
    private RecordHistoryPartRepository partRepository;
    @Autowired
    private RecordRoomRepository roomRepository;
    @Autowired
    private LiveMsgRepository liveMsgRepository;


    public void process(RecordHistory history) throws IOException {
        List<RecordHistoryPart> partList = partRepository.findByHistoryIdOrderByStartTimeAsc(history.getId());
        RecordRoom room = roomRepository.findByRoomId(history.getRoomId());
        String wxuid = room.getWxuid();
        // 初始化 FFmpeg
        FFmpeg ffmpeg = new FFmpeg();
        FFmpegExecutor executor = new FFmpegExecutor(ffmpeg);
        Path outputPath = Path.of(history.getFilePath(), "cut");
        if (taskRunningMsg.get(history.getId()) != null) {
            return;
        }
        try {

            taskRunningMsg.put(history.getId(), "创建输出目录");
            if (!Files.exists(outputPath)) {
                Files.createDirectories(outputPath);
            } else {
                Files.walk(outputPath)
                        .sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(File::delete);
                Files.createDirectories(outputPath);
            }


            taskRunningMsg.put(history.getId(), "开始分片");
            //循环处理每个分片
            int i = 0;
            int count = 0;
            for (RecordHistoryPart part : partList) {
                Map<Integer, Integer> highEnergyCut = getHighEnergyCut(part, room.getPercentileRank());
                List<Integer> cutList = highEnergyCut.keySet().stream().sorted().toList();
                count += cutList.size();
                for (Integer time : cutList) {
                    i++;
                    String output = Path.of(outputPath.toString(), String.format("%05d", i) + "." + part.getFilePath().substring(part.getFilePath().lastIndexOf(".") + 1)).toString();
                    int startTime = time;
                    int duration = highEnergyCut.get(time);

                    FFmpegBuilder builder = new FFmpegBuilder()
                            .setInput(part.getFilePath())
                            .setStartOffset(startTime, TimeUnit.SECONDS)
                            .addExtraArgs("-t", String.valueOf(duration))
                            .overrideOutputFiles(true)
                            .addOutput(output)
                            .setVideoCodec("copy")
                            .setAudioCodec("copy")
                            .done();
                    long currentTimeMillis = System.currentTimeMillis();
                    taskRunningMsg.put(history.getId(), "开始处理分片 " + i);
                    executor.createJob(builder).run();
                    log.info("✅ 生成: {} 片段第 {} 个,共{}个 超过，耗时 {} 秒。", history.getTitle(), i, count, (System.currentTimeMillis() - currentTimeMillis) / 1000);
                }
            }
            // 获取所有文件，按名字排序
            List<String> files;
            try (Stream<Path> stream = Files.list(outputPath)) {
                files = stream
                        .sorted() // 确保顺序：part001, part002...
                        .map(p -> p.toAbsolutePath().toString())
                        .toList();
            }
            if (files.isEmpty()) {
                log.error("没有找到任何文件。");
            }
            // 创建 list.txt
            Path listFile = outputPath.resolve("list.txt");
            StringBuilder sb = new StringBuilder();
            for (String file : files) {
                sb.append("file '").append(file).append("'\n");
            }
            Files.writeString(listFile, sb.toString());
            String output = Path.of(outputPath.toString(), "output.mp4").toString();
            taskRunningMsg.put(history.getId(), "开始合并分片");
            long currentTimeMillis = System.currentTimeMillis();
            try {
                FFmpegBuilder copyBuilder = new FFmpegBuilder()
                        .setInput(listFile.toString())
                        .addExtraArgs("-f", "concat")
                        .addExtraArgs("-safe", "0")
                        .overrideOutputFiles(true)
                        .addOutput(output)
                        .addExtraArgs("-c", "copy")
                        .done();
                executor.createJob(copyBuilder).run();
            } catch (Exception e) {
                FFmpegBuilder encodeBuilder = new FFmpegBuilder()
                        .setInput(listFile.toString())
                        .addExtraArgs("-f", "concat")
                        .overrideOutputFiles(true)
                        .addOutput(output)
                        .addExtraArgs("-safe", "0")
                        .setAudioChannels(2)         // 立体声
                        .setAudioCodec("aac")        // AAC 编码
                        .setAudioSampleRate(48_000)  // 48kHz
                        .setAudioBitRate(128_000)    // 128 kbit/s（标准高质量）
                        .addExtraArgs(
                                "-c:v", "libx264", "-crf", "23", "-preset", "fast",
                                "-c:a", "aac", "-b:a", "128k"
                        )
                        .setVideoCodec("libx264")            // H.264 编码
                        .setVideoFrameRate(60, 1)            // 30 fps（更流畅，也可用 24/25）
                        .setVideoResolution(1920, 1080)      // 1080p 分辨率
                        .setVideoBitRate(5_000_000)          // 5 Mbps 码率（关键！）
                        .done();
                executor.createJob(encodeBuilder).run();
            }
            log.info("✅ 生成: {} 最终视频，耗时 {} 秒。", history.getTitle(), (System.currentTimeMillis() - currentTimeMillis) / 1000);
            if (StringUtils.isNotBlank(wxuid)) {
                Message message = new Message();
                message.setAppToken(wxToken);
                message.setContentType(Message.CONTENT_TYPE_TEXT);
                message.setContent(WX_MSG_FORMAT.formatted("直播剪辑生成视频", room.getUname(), room.getTitle(),
                        LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy年MM月dd日HH点mm分ss秒")), "耗时" + (System.currentTimeMillis() - currentTimeMillis) / 1000) + "秒");
                message.setUid(wxuid);
                WxPusher.send(message);
            }
            taskRunningMsg.put(history.getId(), "开始上传");
            String upload = upload(room, output);
            taskRunningMsg.put(history.getId(), "开始投稿");
            publish(history, upload);
        } catch (Exception e) {
            log.error("剪辑投稿: {} 最终视频失败", history.getTitle(), e);
            if (StringUtils.isNotBlank(wxuid)) {
                Message message = new Message();
                message.setAppToken(wxToken);
                message.setContentType(Message.CONTENT_TYPE_TEXT);
                message.setContent(WX_MSG_FORMAT.formatted("直播剪辑失败", room.getUname(), room.getTitle(),
                        LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy年MM月dd日HH点mm分ss秒")), e));
                message.setUid(wxuid);
                WxPusher.send(message);
            }
        } finally {
            taskRunningMsg.remove(history.getId());
            //删除所有生成的文件
            Files.walk(outputPath)
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
        }
    }

    public void publish(RecordHistory history, String fileName) throws InterruptedException {
        RecordRoom room = roomRepository.findByRoomId(history.getRoomId());
        String wxuid = room.getWxuid();
        Map<String, Object> map = new HashMap<>();
        LocalDateTime startTime = history.getStartTime();
        map.put("date", startTime);

        String uname = room.getUname();
        if (uname == null) {
            map.put("${uname}", "");
        } else {
            map.put("${uname}", uname);
        }
        String title = StringUtils.isNotBlank(history.getTitle()) ? history.getTitle() : "直播录像";
        map.put("${title}", title);
        map.put("${roomId}", room.getRoomId());
        map.put("${areaName}", "");

        SingleVideoDto dto = new SingleVideoDto();
        dto.setTitle(room.getUname() + "直播录像剪辑");
        dto.setDesc("");
        dto.setFilename(fileName);

        VideoUploadDto videoUploadDto = new VideoUploadDto();
        videoUploadDto.setTitle(this.template("【直播剪辑】【${uname}】${title} ${yyyy年MM月dd日HH点mm分}", map));
        videoUploadDto.setTid(room.getTid());
        videoUploadDto.setCover(history.getCoverUrl());
        videoUploadDto.setCopyright(1);
        videoUploadDto.setDesc("直播间: https://live.bilibili.com/" + room.getRoomId() + "  稿件直播源\n自动剪辑投稿技术支持https://space.bilibili.com/10043269");
        videoUploadDto.setVideos(Collections.singletonList(dto));
        videoUploadDto.setTag(this.template(room.getTags(), map));

        Optional<BiliBiliUser> userOptional = biliUserRepository.findById(room.getUploadUserId());
        BiliBiliUser biliBiliUser = userOptional.get();

        String uploadRes = null;
        uploadRes = BiliApi.webPublish(biliBiliUser, videoUploadDto);
        log.info("webPublish uploadRes==>{}", uploadRes);
        if (uploadRes.contains("验证码")) {
            Thread.sleep(120 * 1000L);
            uploadRes = BiliApi.webPublish(biliBiliUser, videoUploadDto);
            log.info("clientPublish uploadRes==>{}", uploadRes);
        }
        String bvid = JSON.parseObject(uploadRes).getJSONObject("data").getString("bvid");
        String aid = JSON.parseObject(uploadRes).getJSONObject("data").getString("aid");
        if (StringUtils.isBlank(bvid) || StringUtils.isBlank(aid)) {
            log.info("发布={}=视频失败 == > {}", room.getUname(), uploadRes);
            if (StringUtils.isNotBlank(wxuid)) {
                Message message = new Message();
                message.setAppToken(wxToken);
                message.setContentType(Message.CONTENT_TYPE_TEXT);
                message.setContent(WX_MSG_FORMAT.formatted("直播剪辑投稿失败", room.getUname(), room.getTitle(),
                        LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy年MM月dd日HH点mm分ss秒")), uploadRes));
                message.setUid(wxuid);
                WxPusher.send(message);
            }
            throw new RuntimeException(uploadRes);
        }
        log.info("发布={}=视频成功 == > {}", room.getUname(), JSON.toJSONString(history));
        if (StringUtils.isNotBlank(wxuid)) {
            Message message = new Message();
            message.setAppToken(wxToken);
            message.setContentType(Message.CONTENT_TYPE_TEXT);
            message.setContent(WX_MSG_FORMAT.formatted("直播剪辑投稿成功", room.getUname(), room.getTitle(),
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy年MM月dd日HH点mm分ss秒")), uploadRes));
            message.setUid(wxuid);
            WxPusher.send(message);
        }
    }

    public String upload(RecordRoom room, String filePath) {

        UploadEnums uploadEnums = UploadEnums.find(room.getLine());
        Optional<BiliBiliUser> userOptional = biliUserRepository.findById(room.getUploadUserId());
        BiliBiliUser biliBiliUser = userOptional.get();
        WebCookie webCookie = Cookie.parse(biliBiliUser.getCookies());

        File uploadFile = new File(filePath);
        Map<String, String> preParams = new HashMap<>();
        preParams.put("r", uploadEnums.getOs());
        preParams.put("profile", uploadEnums.getProfile());
        preParams.put("name", uploadFile.getName());
        preParams.put("size", String.valueOf(uploadFile.length()));
        long fileSize = uploadFile.length();
        long chunkSize = 1024 * 1024 * 5;
        long chunkNum = (long)Math.ceil((double)fileSize / chunkSize);
        PreUploadRequest preuploadRequest = new PreUploadRequest(webCookie, preParams);
        preuploadRequest.setLineQuery(uploadEnums.getLineQuery());
        PreUploadBean preUploadBean;
        LineUploadBean uploadBean = null;
        try {
            do {
                preUploadBean = preuploadRequest.getPojo();
                if (preUploadBean == null || preUploadBean.getOK() == 0) {
                    try {
                        log.info("上传限流等待十秒==>{}", uploadFile.getName());
                        Thread.sleep(10000L);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                } else {
                    // 同步更新
                    //                                    chunkSize = preUploadBean.getChunk_size();
                    //                                    chunkNum = (long) Math.ceil((double) fileSize / chunkSize);
                    // 如果返回的线路不是指定的线路，则从备用线路选择
                    if (!preUploadBean.getEndpoint().contains(("upcdn" + uploadEnums.getCdn()))) {
                        String[] endpoints = preUploadBean.getEndpoints();
                        for (String endpoint : endpoints) {
                            if (endpoint.contains("upcdn" + uploadEnums.getCdn())) {
                                preUploadBean.setEndpoint(endpoint);
                            }
                        }
                    }
                    LineUploadRequest uploadRequest = new LineUploadRequest(webCookie, preUploadBean);
                    uploadBean = uploadRequest.getPojo();
                    log.error("preUploadBean==>{}\nuploadBean==>{}", JSON.toJSONString(preUploadBean), JSON.toJSONString(uploadBean));
                }
            } while (preUploadBean.getOK() == 0);
        } catch (Exception e) {
            throw new RuntimeException("并发上传失败，存在异常", e);
        }
        // 分段上传
        AtomicInteger upCount = new AtomicInteger(0);
        AtomicInteger tryCount = new AtomicInteger(0);
        List<Runnable> runnableList = new ArrayList<>();
        for (int i = 0; i < chunkNum; i++) {
            long finalI = i;
            LineUploadBean finalUploadBean = uploadBean;
            PreUploadBean finalPreUploadBean = preUploadBean;
            Runnable runnable = () -> {
                try {
                    while (tryCount.get() < 200) {
                        try {
                            // 上传
                            long endSize = (finalI + 1) * chunkSize;
                            long finalChunkSize = chunkSize;
                            Map<String, String> chunkParams = new HashMap<>();
                            chunkParams.put("partNumber", String.valueOf(finalI + 1));
                            chunkParams.put("uploadId", finalUploadBean.getUpload_id());
                            chunkParams.put("chunk", String.valueOf(finalI));
                            chunkParams.put("chunks", String.valueOf(chunkNum));
                            chunkParams.put("size", String.valueOf(finalChunkSize));
                            chunkParams.put("start", String.valueOf(finalI * finalChunkSize));
                            chunkParams.put("end", String.valueOf(endSize));
                            chunkParams.put("total", String.valueOf(fileSize));
                            if (endSize > fileSize) {
                                endSize = fileSize;

                                finalChunkSize = fileSize - (finalI * finalChunkSize);
                                chunkParams.put("size", String.valueOf(finalChunkSize));
                                chunkParams.put("end", String.valueOf(endSize));
                            }
                            try {
                                ChunkUploadRequest chunkUploadRequest = new ChunkUploadRequest(finalPreUploadBean, chunkParams, new RandomAccessFile(filePath, "r"));
                                chunkUploadRequest.getPage();
                            } catch (FileNotFoundException fileNotFoundException) {
                                tryCount.set(200);
                                log.error("上传失败，{}文件不存在", filePath);
                                break;
                            }
                            int count = upCount.incrementAndGet();
                            log.info("{}==>[{}] 上传视频part {} 进度{}/{}", Thread.currentThread().getName(), room.getTitle(),
                                    filePath, count, chunkNum);
                            break;
                        } catch (Exception e) {
                            tryCount.incrementAndGet();
                            log.info("{}==>[{}] 上传视频part {}, index {}, size {}, start {}, end {}, exception={}", Thread.currentThread().getName(), room.getTitle(),
                                    filePath, finalI, chunkSize, finalI * chunkSize, (finalI + 1) * chunkSize, ExceptionUtils.getStackTrace(e));
                            try {
                                //                                                log.info("上传失败等待十秒==>{}", uploadFile.getName());
                                Thread.sleep(10000L);
                            } catch (InterruptedException ex) {
                                ex.printStackTrace();
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            };

            runnableList.add(runnable);

        }

        runnableList.stream().parallel().forEach(Runnable::run);
        //通知服务器上传完成
        Map<String, String> completeParams = new HashMap<>();
        completeParams.put("profile", uploadEnums.getProfile());
        completeParams.put("name", uploadFile.getName());
        completeParams.put("uploadId", uploadBean.getUpload_id());
        completeParams.put("biz_id", String.valueOf(preUploadBean.getBiz_id()));
        Map<String, Object> bodyMap = new LinkedHashMap<>(1);
        List<Map<String, String>> chunkMaps = new ArrayList<>((int)chunkNum);
        for (int i = 1; i <= chunkNum; i++) {
            Map<String, String> partMap = new LinkedHashMap<>(2);
            partMap.put("partNumber", String.valueOf(i));
            partMap.put("eTag", "etag");
            chunkMaps.add(partMap);
        }
        bodyMap.put("parts", chunkMaps);
        CompleteUploadRequest completeUploadRequest = new CompleteUploadRequest(preUploadBean, completeParams, JSON.toJSONString(bodyMap));

        try {
            CompleteUploadBean completeUploadBean = null;
            for (int i = 0; i < 5; i++) {
                try {
                    completeUploadBean = completeUploadRequest.getPojo();
                } catch (Exception e) {
                    if (completeUploadBean == null) {
                        completeUploadBean = new CompleteUploadBean();
                    }
                    log.error("{},文件合并失败，准备重试", filePath, e);
                }
                if (completeUploadBean != null && completeUploadBean.getOK() != null && completeUploadBean.getOK() == 1) {
                    break;
                }
            }

            if (completeUploadBean != null && completeUploadBean.getOK() != null && completeUploadBean.getOK() == 1) {
                return uploadBean.getFileName();
            } else {
                throw new RuntimeException("合并上传文件失败：" + JSON.toJSONString(completeUploadBean));
            }

        } catch (Exception e) {
            throw new RuntimeException("合并上传文件失败：" + e.getMessage());
        }
    }

    public Map<Integer, Integer> getHighEnergyCut(RecordHistoryPart part, double percentileRank) {
        // 1. 获取每秒的弹幕数
        List<Object[]> highEnergyCuts = liveMsgRepository.getMsgCountBySecond(part.getId());
        List<HighEnergyCut> msgCountBySecond = new ArrayList<>();
        TreeMap<Integer, Integer> timeToMsgCount = new TreeMap<>();
        for (Object[] row : highEnergyCuts) {
            int time = ((Number)row[0]).intValue();  // 时间戳（秒）
            int num = ((Number)row[1]).intValue();   // 该秒的弹幕数
            msgCountBySecond.add(new HighEnergyCut(time, num));
            timeToMsgCount.put(time, num);
        }

        if (msgCountBySecond.isEmpty()) {
            return Collections.emptyMap();
        }

        // 2. 按时间排序并构建前缀和数组
        List<Integer> sortedTimes = msgCountBySecond.stream()
                .map(HighEnergyCut::getTime)
                .sorted()
                .collect(Collectors.toList());

        int n = sortedTimes.size();
        int[] prefixSum = new int[n + 1];  // prefixSum[0] = 0

        for (int i = 0; i < n; i++) {
            prefixSum[i + 1] = prefixSum[i] + msgCountBySecond.get(i).getNum();
        }
        int maxTime = timeToMsgCount.lastKey();

        // 生成连续时间序列（从 minTime 到 maxTime）
        List<Integer> allTimes = new ArrayList<>();
        for (int t = 0; t <= maxTime; t++) {
            allTimes.add(t);
        }
        //初始化0秒时的数据
        Map<Integer, Integer> smoothedValues = new HashMap<>();
        int windowSum = 0;
        // 滑动窗口左边界
        int left = -10;
        // 滑动窗口右边界
        int right = 10;

        // 初始窗口填充
        for (int t = left; t <= right; t++) {
            windowSum += timeToMsgCount.getOrDefault(t, 0);
        }
        smoothedValues.put(0, windowSum);

        // 滑动窗口增量更新
        for (int i = 1; i < allTimes.size(); i++) {
            int prevTime = allTimes.get(i - 1);
            int currTime = allTimes.get(i);

            // 移除滑出窗口的旧元素
            int outTime = prevTime - 10;
            windowSum -= timeToMsgCount.getOrDefault(outTime, 0);

            // 添加滑入窗口的新元素
            int inTime = currTime + 10;
            windowSum += timeToMsgCount.getOrDefault(inTime, 0);

            smoothedValues.put(currTime, windowSum);
        }
        // 4. 计算分位数
        List<Integer> smoothedNums = new ArrayList<>(smoothedValues.values());
        Collections.sort(smoothedNums);
        double percentile = calculatePercentile(smoothedNums, percentileRank);
        if (percentile <= 1) {
            percentile = 1;
        }
        // 5. 筛选高能点
        List<Integer> highEnergySeconds = new ArrayList<>();
        for (Map.Entry<Integer, Integer> entry : smoothedValues.entrySet()) {
            if (entry.getValue() >= percentile) {
                highEnergySeconds.add(entry.getKey());
            }
        }
        Collections.sort(highEnergySeconds);

        if (highEnergySeconds.isEmpty()) {
            return Collections.emptyMap();
        }


        // 6. 原始合并（基于连续时间点）
        List<int[]> originalIntervals = new ArrayList<>();
        int start = highEnergySeconds.get(0);
        int end = start;

        for (int i = 1; i < highEnergySeconds.size(); i++) {
            int current = highEnergySeconds.get(i);
            int prev = highEnergySeconds.get(i - 1);

            if (current - prev == 1) {  // 仅合并连续时间点
                end = current;
            } else {
                originalIntervals.add(new int[]{start, end});
                start = current;
                end = current;
            }
        }
        originalIntervals.add(new int[]{start, end});

        // 7. 时间调整（前推30秒，延长10秒）
        List<int[]> adjustedIntervals = new ArrayList<>();
        for (int[] interval : originalIntervals) {
            int originalStart = interval[0];
            int originalEnd = interval[1];
            int adjustedStart = originalStart - 30;  // 前推30秒
            int adjustedEnd = originalEnd + 40;      // 延长10秒
            adjustedIntervals.add(new int[]{adjustedStart, adjustedEnd});
        }

        // 8. 二次合并（基于调整后的区间，间隔 ≤ 60 秒）
        List<int[]> mergedIntervals = new ArrayList<>();
        if (!adjustedIntervals.isEmpty()) {
            int[] currentInterval = adjustedIntervals.get(0);
            for (int i = 1; i < adjustedIntervals.size(); i++) {
                int[] nextInterval = adjustedIntervals.get(i);
                int prevEnd = currentInterval[1];
                int nextStart = nextInterval[0];

                if (nextStart - prevEnd <= 60) {  // 间隔 ≤ 60 秒，合并
                    currentInterval[1] = Math.max(currentInterval[1], nextInterval[1]);
                } else {
                    mergedIntervals.add(currentInterval);
                    currentInterval = nextInterval;
                }
            }
            mergedIntervals.add(currentInterval);
        }

        // 9. 转换为 Map<起始秒, 持续时间>
        Map<Integer, Integer> result = new LinkedHashMap<>();
        for (int[] interval : mergedIntervals) {
            int startTime = interval[0];
            int duration = interval[1] - interval[0] + 1;
            if (startTime < 0) {
                startTime = 0;
            }
            result.put(startTime, duration);
        }
        return result;
    }

    // 分位数计算（与原代码相同）
    private double calculatePercentile(List<Integer> sortedValues, double percentileRank) {
        int n = sortedValues.size();
        if (n == 0) {
            return 0;
        }
        if (n <= 60 * 5) {
            // 小于五分钟的视频无需合并
            return 999999;
        }

        // 确保 percentileRank 在 [0, 1] 范围内
        percentileRank = Math.max(0.0, Math.min(1.0, percentileRank));

        double index = (n - 1) * percentileRank + 1;
        int floor = (int)Math.floor(index); // 下取整
        int ceil = floor + 1;

        // 修正：确保 floor 和 ceil 在有效范围内
        floor = Math.max(0, Math.min(n - 1, floor));
        ceil = Math.max(0, Math.min(n - 1, ceil));

        double fraction = index - floor;
        return sortedValues.get(floor) + fraction * (sortedValues.get(ceil) - sortedValues.get(floor));
    }

    private String template(String template, Map<String, Object> map) {
        template = template.replace("${uname}", map.get("${uname}") != null ? map.get("${uname}").toString() : "")
                .replace("${title}", map.get("${title}") != null ? map.get("${title}").toString() : "")
                .replace("${index}", map.get("${index}") != null ? map.get("${index}").toString() : "")
                .replace("${areaName}", map.get("${areaName}") != null ? map.get("${areaName}").toString() : "")
                .replace("${roomId}", map.get("${roomId}") != null ? map.get("${roomId}").toString() : "");
        if (template.contains("${")) {
            try {
                LocalDateTime localDateTime = (LocalDateTime)map.get("date");
                String substring = template.substring(template.indexOf("${"));
                String date = substring.substring(0, substring.indexOf("}") + 1);
                String format = localDateTime.format(DateTimeFormatter.ofPattern(date.substring(2, date.length() - 1)));
                template = template.replace(date, format);
            } catch (Exception e) {
                log.error("时间格式模板失败：" + template);
            }
        }
        template = template.replace(",,", ",");
        return template;
    }
}
