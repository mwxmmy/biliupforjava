# 基于blrec webhook 的自动投稿工具

用于b站录播姬，mikufans录播姬，blrec的，
基于WebHook的全自动上传投稿工具

本程序只负责录播文件上传投稿，不支持录制

本上传插件特点

**支持录制一个文件上传一个文件，减少硬盘使用空间**

**支持直播封面和自定义封面**

**支持直播弹幕转移到录播视频里（非压制进视频）(可能有风险)**

**支持上传到b站云剪辑平台素材库进行剪辑投稿**

**支持文件处理完后移动，可配合rclone,webdav上传到云盘**

**适用于：NAS、服务器直播监控、录制及自动投稿，适配Windows 和 linux**

# 录播投稿使用流程

群号：697605055 <a target="_blank" href="https://qm.qq.com/cgi-bin/qm/qr?k=kBA4u6rVFe_n2XjyYGx94CgTh3-KWM5T&jump_from=webapi&authKey=nhTa8F4D31bovL/ZwEfX5Qt148AyzJKCD4cC0+6ew/Y8bJfcf6aJKxtqXPUjQpwx"><img border="0" src="//pub.idqqimg.com/wpa/images/group.png" alt="录播姬投稿插件交流群" title="录播姬投稿插件交流群"></a>

首先说明，本方法支持docker镜像方式部署。
本地化部署方式需要安装java17环境，具体方法为安装配置java17环境，下载运行包。进入运行包执行运行命令。

```
运行文件下载地址持续更新：
https://www.aliyundrive.com/s/8n4waHuh5sA

java -Dserver.port=端口号 -Drecord.work-path=录播姬工作目录 -Drecord.userName=登录用户名 -Drecord.password=登录密码  -Duser.timezone=Asia/Shanghai -jar 程序文件名

示例
java -Dserver.port=12380  -Drecord.work-path=E:\rec -Drecord.userName=root -Drecord.password=123  -Duser.timezone=Asia/Shanghai -Dfile.encoding=UTF-8 -jar biliupfojava.war

录播姬配置webhookv2
http://127.0.0.1:端口号/recordWebHook
blrec配置webhook
http://127.0.0.1:端口号/recordWebHook

```

## 安装录播姬

录播姬使用教程参考官网https://rec.danmuji.org/

```
docker pull bililive/recorder:latest
docker run -d -v 宿主机路径:/rec -p 宿主机端口:2356 --name brec  --restart always   bililive/recorder

录播姬推荐文件格式（自用），可自行修改
{{ roomId }}-{{ name }}/{{ json.room_info.live_start_time | time_zone: "Asia/Shanghai" | format_date: "yyyy年" }}/{{ json.room_info.live_start_time | time_zone: "Asia/Shanghai" | format_date: "MM月" }}/{{ json.room_info.live_start_time | time_zone: "Asia/Shanghai" | format_date: "dd号" }}/{{ json.room_info.live_start_time | time_zone: 'Asia/Shanghai' | format_date: "yyyy年MM月dd号-HH点mm分ss秒开播" }}/{{ title }}-{{ "now" | time_zone: 'Asia/Shanghai' | format_date: "yyyy年MM月dd号-HH点mm分ss秒" }}-{{ partIndex | format_number: "000" }}.flv
```

## 安装blrec

录播姬使用教程参考官网https://github.com/acgnhiki/blrec

```
docker pull bililive/recorder:latest
docker run -d --name blrec -v 宿主机路径/rec:/rec  -p 宿主机端口:2233 --restart always  acgnhiki/blrec

推荐文件格式（自用），可自行修改
{roomid} - {uname}/{year}年/{month}月/{day}号/{year}年{month}月{day}号{hour}{minute}{second}
```

## 安装上传插件

注意，此处的宿主机路径和录播姬的应一致。
如为blrec,此处的宿主机路径需要添加blrec文件存放目录，默认为/rec,则 宿主机路径/rec 对应 blrec 容器内存放路径 /rec
宿主机路径/xxxx/zzz 对应 blrec容器内存放路径 /xxxx/zzz

端口随意，注意默认无安全设置，可自定义账密登录，最好不要暴露公网，通过ssh隧道访问比较好
关于内存配置，插件最低内存需求为400M,若内存充足尽量配置512M以上或不配置限制

```
docker pull mwxmmy/biliupforjava
docker run -p 宿主机端口:80 -v 宿主机路径:/bilirecord --name bup -d --restart always mwxmmy/biliupforjava

内存配置在docker run 参数添加 -m 512M
如下
docker run -d  -p 宿主机端口:80 -v 宿主机路径:/bilirecord --name bup   --restart always -m 512M mwxmmy/biliupforjava

配置账密登录添加参数  -e JAVA_OPTS='-Drecord.userName=登录用户名 -Drecord.password=登录密码' 
如下
docker run -d  -p 宿主机端口:80 -v 宿主机路径:/bilirecord --name bup  -e JAVA_OPTS='-Drecord.userName=登录用户名 -Drecord.password=登录密码' --restart always -m 512M mwxmmy/biliupforjava
```

## 配置网络

```
需要配置录播姬和插件属于同一网络才能正常运行
创建网络并且连接录播姬和插件容器
docker network create bili-net
docker network connect bili-net brec
docker network connect bili-net bup
```

## 录播姬配置webhookv2

```
打开录播姬webui页面，如配置的宿主机端口是2356 通过ssh转发本地端口访问或局域网直接访问
修改录播姬webhookv2配置为： 注意bup为 插件容器名称
http://bup/recordWebHook
若未按教程配置网络则通过局域网ip形式访问。
可直接在浏览器访问该地址判断是否正常。
注意ip可能会变动导致无法访问。
切记不要使用localhost或容器ip来配置。
示例：
http://192.168.0.32:12380/recordWebHook
```

## 上传插件配置

```
同理打开webui页面，首先点击用户页面进行登录
第一次添加初始化后配置直播间内容，如是否删除文件，上传用户，是否上传等。直播间配置的是否上传只会影响后续的重新录播内容。
```

## 系统截图

[![zjCFN6.md.png](https://s1.ax1x.com/2022/12/23/zjCFN6.md.png)](https://imgse.com/i/zjCFN6)
[![zjCiAx.md.png](https://s1.ax1x.com/2022/12/23/zjCiAx.md.png)](https://imgse.com/i/zjCiAx)
[![zjCk4K.md.png](https://s1.ax1x.com/2022/12/23/zjCk4K.md.png)](https://imgse.com/i/zjCk4K)
