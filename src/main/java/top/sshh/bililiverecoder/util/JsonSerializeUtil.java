package top.sshh.bililiverecoder.util;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import java.io.*;
import java.util.Deque;
import java.util.LinkedList;

/**
 * 提供JSONObject的序列化和反序列化，与通过路径方式对JSON进行读取与写入
 */
public class JsonSerializeUtil {


    public static boolean isExistsCacheToFile(String name) {
        return new File("./data/cache/" + name).exists();
    }

    public static boolean isExists(String name) {
        return new File(name).exists();
    }

    /**
     * 序列化缓存到缓存目录
     *
     * @param jsonObject JSONObject 对象
     * @param name       文件名称(注意:不是路径!!!)
     */
    public static synchronized void writeCacheToFile(JSONObject jsonObject, String name) throws Exception {
        write0(jsonObject, "./data/cache/" + name, "UTF-8");

    }

    /**
     * 序列化缓存到缓存目录
     *
     * @param jsonObject  JSONObject 对象
     * @param name        文件名称(注意:不是路径!!!)
     * @param charsetName 编码
     */
    public static synchronized void writeCacheToFile(JSONObject jsonObject, String name, String charsetName) throws Exception {
        write0(jsonObject, "./data/cache/" + name, charsetName);
    }

    /**
     * 序列化
     *
     * @param jsonObject JSONObject对象
     * @param file       File对象
     */
    public static synchronized void write(JSONObject jsonObject, File file) throws Exception {
        write0(jsonObject, file.getPath(), "UTF-8");
    }

    /**
     * 序列化
     *
     * @param jsonObject  JSONObject对象
     * @param file        File 对象
     * @param charsetName charsetName 编码格式
     */
    public static synchronized void write(JSONObject jsonObject, File file, String charsetName) throws Exception {
        write0(jsonObject, file.getPath(), charsetName);
    }

    /**
     * 序列化
     *
     * @param jsonObject JSONObject对象
     * @param filePath   文件路径
     */
    public static synchronized void write(JSONObject jsonObject, String filePath) throws Exception {
        write0(jsonObject, filePath, "UTF-8");
    }


    /**
     * 序列化
     *
     * @param jsonObject  JSONObject 对象
     * @param filePath    文件路径
     * @param charsetName 编码格式
     */
    public static synchronized void write(JSONObject jsonObject, String filePath, String charsetName) throws Exception {
        write0(jsonObject, filePath, charsetName);
    }


    /**
     * 反序列化
     *
     * @param name 文件名称
     * @return json字符串
     */
    public static synchronized String readCache(String name) throws Exception {
        return read0("./data/cache/" + name);
    }

    /**
     * 反序列化
     *
     * @param name 文件名称
     * @return json字符串
     */
    public static synchronized byte[] readCacheToByte(String name) throws Exception {
        return readByte("./data/cache/" + name);
    }

    /**
     * 反序列化
     *
     * @param name 文件名称
     * @return json字符串
     */
    public static synchronized JSONObject readCacheToJSONObject(String name) throws Exception {
        return JSONObject.parseObject(read0("./data/cache/" + name));
    }


    /**
     * 反序列化
     *
     * @param path 文件名称
     * @return json字符串
     */
    public static synchronized String read(String path) throws Exception {
        return read0(path);
    }

    /**
     * 反序列化
     *
     * @param path 文件名称
     * @return json字符串
     */
    public static synchronized byte[] readToByte(String path) throws Exception {
        return readByte(path);
    }

    /**
     * 反序列化
     *
     * @param path 文件名称
     * @return json字符串
     */
    public static synchronized JSONObject readToJSONObject(String path) throws IOException {
        return JSONObject.parseObject(read0(path));
    }


    /**
     * 读文件
     *
     * @param path 文件路径
     * @return json字符串
     */
    private static String read0(String path) throws IOException {
        FileInputStream fileInputStream = null;
        try {
            fileInputStream = new FileInputStream(path);

            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            byte[] b = new byte[32 * 1024];
            int code = 0;
            while ((code = fileInputStream.read(b)) != -1) {
                byteArrayOutputStream.write(b, 0, code);
            }
            return byteArrayOutputStream.toString();
        } finally {
            if (fileInputStream != null)
                fileInputStream.close();
        }
    }

    /**
     * 读文件
     *
     * @param path 文件路径
     * @return json字符串
     */
    private static byte[] readByte(String path) throws Exception {
        FileInputStream fileInputStream = null;
        try {
            fileInputStream = new FileInputStream(path);

            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            byte[] b = new byte[32 * 1024];
            int code = 0;
            while ((code = fileInputStream.read(b)) != -1) {
                byteArrayOutputStream.write(b, 0, code);
            }
            return byteArrayOutputStream.toByteArray();
        } finally {
            if (fileInputStream != null)
                fileInputStream.close();
        }
    }

    /**
     * 序列化
     *
     * @param jsonObject  JSONObject 对象
     * @param filePath    文件路径
     * @param charsetName 编码格式
     */
    private static synchronized void write0(JSONObject jsonObject, String filePath, String charsetName) throws Exception {
        File file = new File(filePath);
        file.setReadable(true, true);
        file.setWritable(true, true);
        if (!file.exists()) {
            file.getParentFile().mkdirs();
        }

        FileOutputStream fileOutputStream = null;
        try {
            fileOutputStream = new FileOutputStream(file);
            fileOutputStream.write(jsonObject.toString().getBytes(charsetName));
            fileOutputStream.flush();
        } finally {
            if (fileOutputStream != null)
                fileOutputStream.close();
        }
    }

    public static Path getJsonPath() {
        return new Path();
    }


    /**
     * json解析类
     */
    public static class Path {

        public static void main(String[] args) {
            JSONObject jsonObject = JSONObject.parseObject("{\n" +
                    "  \"type\": 1}");
            JsonSerializeUtil.getJsonPath().write(jsonObject, "/type", "吃个桃桃", true);
        }


        /**
         * @param json    json对象
         * @param xpath   路径
         * @param param   要添加的值
         * @param isIndex 路径顶端是否是索引，如果是索引请至为true
         * @注意 对JSON数组未能完全的适配，如果直接访问数组索引那么会有奇怪的bug，但是可以访问数组索引中的对象key。所以如果路径顶是数组索引请指定index
         */
        public Object write(JSONObject json, String xpath, Object param, boolean isIndex) {
            Deque<Object> deque = new LinkedList<>();//栈 json
            Deque<String> dequeKey = new LinkedList<>();//栈 jsonName


            String[] split = xpath.split("/");

            //将路经入栈
            Object jsonRoot = json;
            int i = 0;
            for (String s : split) {
                if ("".equals(s)) continue;
                String read = read(jsonRoot, s);
                if (isObject(read)) {
                    jsonRoot = JSONObject.parseObject(read);
                    deque.push(jsonRoot);//入栈
                    dequeKey.push(s);
                } else if (isArray(read)) {
                    jsonRoot = JSONArray.parseArray(read);
                    deque.push(jsonRoot);//入栈
                    dequeKey.push(s);
                } else {
                    deque.push(read);//入栈
                    dequeKey.push(s);
                }
            }

            //路径出栈并修改值
            boolean a = true;
            Object root = param;
            for (Object o : deque) {
                if (o instanceof JSONObject) {
                    JSONObject o1 = (JSONObject) o;
                    if (a && isIndex) {
                        //如果路径的顶部是数组的索引，并且是第一次进入循环，那么就对改索引除的值(这值通常是对象)进行替换
                        root = o1;
                        a = false;
                    } else {
                        //如果不是就在对象内添加元素
                        o1.put(dequeKey.pop(), root);
                    }
                    a = false;
                    root = o1;
                } else if (o instanceof JSONArray) {
                    a = false;
                    JSONArray o1 = (JSONArray) o;
                    try {
                        //对改索引的元素进行替换
                        o1.set(Integer.parseInt(dequeKey.peek()), root);
                        dequeKey.pop();
                    } catch (Exception e) {
                        //如果发生异常就添加，而不是替换
                        o1.add(root);
                    }
                    root = o1;
                }
            }

            //如果jsonName栈里面没有名字了就直接返回
            if (dequeKey.isEmpty()) {
                return root;
            } else {
                //否则就添加名称后返回
                JSONObject jsonObject1 = new JSONObject();
                jsonObject1.put(dequeKey.pop(), root);
                return jsonObject1;
            }
        }


        /**
         * 从json对象里拿出指定路径的json对象
         *
         * @param json  JSONObject对象
         * @param xpath 路径,路径途中一点要是json对象,路径的尽头一定要是json对象否则会报错 如 msg/heads
         * @return
         */
        public JSONObject readObject(JSONObject json, String xpath) {
            try {
                return JSONObject.parseObject(read(json, xpath));
            } catch (NullPointerException e) {
                throw new NullPointerException("XPath路径指向目标无效");
            }
        }

        /**
         * 从json对象里拿出指定路径的json数组
         *
         * @param json  JSONObject对象
         * @param xpath 路径
         * @return
         */
        public JSONArray readArray(JSONObject json, String xpath) {
            try {
                return JSONObject.parseArray(read(json, xpath));
            } catch (NullPointerException e) {
                throw new NullPointerException("XPath路径指向目标无效");
            }
        }


        public String read(String json, String xpath) {
            Object jsonObj = (isArray(json) ? JSONArray.parse(json) : JSONObject.parse(json));
            return read(jsonObj, xpath);
        }

        public String read(JSON json, String xpath) {
            return read((Object) json, xpath);
        }

        /**
         * 从路径里拿值
         *
         * @param json  JSONObject对象 或是 JSONArray 对象
         * @param xpath 路径
         * @return
         */
        public String read(Object json, String xpath) {
            String[] split = xpath.split("/");
            JSONObject jsonObject = null;
            JSONArray jsonArray = null;
            //赋初值
            if (json instanceof JSONObject) {
                jsonObject = (JSONObject) json;
            } else if (json instanceof JSONArray) {
                jsonArray = (JSONArray) json;
            } else {
                throw new IllegalArgumentException("传入的不是一个JSOn对象或JSOn数组");
            }

            for (String s : split) {// ['' ]
                String s1 = null;
                //如果路径的前后都是空串就跳过
                if ("".equals(s)) {
                    continue;
                }
                /**
                 * 从路径中获取对象/数组
                 */
                try {
                    if (jsonObject != null) {
                        s1 = jsonObject.get(s).toString();
                    }
                    if (jsonArray != null) {
                        s1 = jsonArray.get(Integer.parseInt(s)).toString();
                    }
                } catch (NullPointerException e) {
                    throw new IllegalArgumentException("xjson 路径不正确导致参数无法正常的被获取\n无参数：" + s + "\n完整xjson: " + xpath + " \n完整 json :" + json, e);
                }
                /**
                 * 推进路径，让获取到的当前路径的值，作为下一次获取路径的依据
                 */
                if (isArray(s1)) {
                    jsonArray = JSONObject.parseArray(s1);
                    jsonObject = null;
                    continue;
                }

                if (isObject(s1)) {
                    jsonObject = JSONObject.parseObject(s1);
                    jsonArray = null;
                    continue;
                }

                if (isVal(s1)) {
                    return s1;
                }
            }

            if (jsonArray != null) return jsonArray.toString();

            return jsonObject.toString();
        }

        public boolean isObject(String json) {
            if (json.startsWith("{") && json.endsWith("}")) {
                //是对象
                return true;
            }
            return false;
        }

        public boolean isArray(String json) {
            if (json.startsWith("[") && json.endsWith("]")) {
                //是数组
                return true;
            }
            return false;
        }

        public boolean isVal(String json) {
            if (json.startsWith("[") && json.endsWith("]")) {
                return false;
            } else if (json.startsWith("{") && json.endsWith("}")) {
                //是对象
                return false;
            }
            return true;
        }
    }

}
