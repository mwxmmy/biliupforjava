package top.sshh.bililiverecoder.util;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TaskUtil {
    public static final Map<Long, Thread> partUploadTask = new ConcurrentHashMap<>();
    public static final Map<Long, Thread> publishTask = new ConcurrentHashMap<>();
}
