package com.jimmy;

import com.jimmy.bean.Barrage;
import com.jimmy.bean.Request;
import com.jimmy.bean.ServerInfo;
import com.jimmy.handler.MessageHandler;
import com.jimmy.handler.ResponseParser;
import com.jimmy.thread.KeepLiveThread;
import com.jimmy.thread.SendBarrage;
import com.jimmy.util.HttpRequest;
import com.jimmy.util.MD5Util;
import com.jimmy.util.SttCode;
import com.jimmy.util.TimeHelper;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 *
 * @author jimmy
 * @desc main
 * @date 2017/10/11
 */
public class Main {

    private static Logger logger = Logger.getLogger(Main.class);
    private static List<ServerInfo> danmakuServers = new ArrayList<>();
    private static int rid = -1; //房间id
    private static int gid = -1;
    private static String roomUrl = "";


    public static void main(String[] args) {
        PropertyConfigurator.configure("src/main/resources/log/log4j.properties");
        logger.info("");
//        roomUrl = "http://www.douyu.com/669746";
        roomUrl = "http://www.douyu.com/cave";
        getInf(roomUrl);
    }

    public static void getInf(String roomUrl) {
        logger.info("获取房间页面 ...");
        String pageHtml =  HttpRequest.get(roomUrl);

        //获取roomId
        logger.info("获取直播房间ID ...");
        rid = ResponseParser.parseRoomId(pageHtml);

        //检查是否在线
        boolean online = ResponseParser.parseOnline(pageHtml);
//        if (!online) {
//            logger.info("该房间还没有直播！" + roomUrl);
//            return;
//        }

        //获取服务器IP列表
        logger.info("获取服务器列表 ...");
        List<ServerInfo> serverList = ResponseParser.parseServerInfo(pageHtml);

        if (serverList == null || serverList.size() <= 0) {
            logger.error("获取服务器列表失败！");
            return;
        }

        Main main = new Main();
        //登陆服务器
        main.loginRequest(serverList);

        if (rid == -1 || gid == -1) {
            return;
        }

        //开始抓取弹幕
        main.startCrawler();
    }

    private void loginRequest(List<ServerInfo> ipList) {
        Socket socket = null;

        ServerInfo server = ipList.get((int) (Math.random() * ipList.size()));

        try {
            logger.info("连接服务器 " + server.getHost() + ":" + server.getPort());
            socket = new Socket(server.getHost(), server.getPort());

            String timestamp = String.valueOf(System.currentTimeMillis() / 1000);
            String uuid = UUID.randomUUID().toString().replace("-", "").toUpperCase();
            String vk = MD5Util.encrypt(timestamp + "7oE9nPEG9xXV69phU31FYCLUagKeYtsF" + uuid);
            //发送登陆请求
            MessageHandler.send(socket, Request.gid(rid, uuid, timestamp, vk));
            //等待接收
            MessageHandler.receive(socket, loginListener);

        } catch (IOException e) {
            logger.debug("Error" + e.toString());
            logger.error("登陆到服务器失败！");
        } finally {
            if (socket != null)
                try {
                    socket.close();
                } catch (IOException e) {
                    logger.debug("Error" + e.toString());
                    logger.error("连接关闭异常！");
                }
        }
    }

    //请求登陆服务器回调
    private MessageHandler.OnReceiveListener loginListener = new MessageHandler.OnReceiveListener() {
        private boolean finish = false;
        private TimeHelper helper = new TimeHelper(10 * 1000);//10秒等待超时

        @Override
        public void onReceive(List<String> responses) {
            boolean f1 = false, f2 = false;

            for (String response : responses) {
                logger.debug("Receive Response" + response);
                if (response.contains("msgrepeaterlist")) {
                    //获取弹幕服务器地址
                    logger.info("获取弹幕服务器地址 ...");
                    String danmakuServerStr = SttCode.deFilterStr(SttCode.deFilterStr(response));
                    danmakuServers = ResponseParser.parseDanmakuServer(danmakuServerStr);
                    logger.info("获取到 " + danmakuServers.size() + " 个服务器地址 ...");
                    f1 = true;
                }

                if (response.contains("setmsggroup")) {
                    //获取gid
                    logger.info("获取弹幕群组ID ...");
                    gid = ResponseParser.parseID(response);
                    logger.info("弹幕群组ID：" + gid);
                    f2 = true;
                }
            }

            //获取到弹幕服务器地址和gid 或者超时之后，结束此次监听
            finish = f1 && f2 || helper.checkTimeout();
        }

        @Override
        public boolean isFinished() {
            return finish;
        }
    };

    private MessageHandler.OnReceiveListener danmakuListener = new MessageHandler.OnReceiveListener() {

        private boolean finished = false;
        private List<Barrage> barrages = new ArrayList<>();
        private TimeHelper helper = new TimeHelper(20 * 60 * 1000);//间隔20min检测一次直播状态

        @Override
        public void onReceive(List<String> responses) {

            for (String response : responses) {
                logger.debug("Receive Response" + response);

                if (!response.contains("chatmsg")) continue;

                //解析弹幕
                Barrage barrage = ResponseParser.parseDanmaku(response);
                if (barrage == null) continue;

                barrages.add(barrage);
                logger.info("barrage:\t" + barrage.getSnick() + ":" + barrage.getContent());

//                if (danmakus.size() >= 20 && DanmakuDao.saveDanmaku(danmakus)) {
//                    logger.i("DB", "保存弹幕到数据库 ...");
//                    danmakus.clear();
//                }
            }

            //检测不在直播， 结束抓取
            if (helper.checkTimeout() && !isOnline()) finished = true;

        }

        @Override
        public boolean isFinished() {
            return finished;
        }
    };

    private void startCrawler() {
        try {

            if (danmakuServers == null || danmakuServers.size() <= 0) {
                logger.error("没有可用的弹幕服务器 ...");
                return;
            }
            ServerInfo danmakuServer = danmakuServers.get((int) (Math.random() * danmakuServers.size()));
            Socket socket = new Socket(danmakuServer.getHost(), danmakuServer.getPort());
            logger.info("登陆到弹幕服务器 " + danmakuServer.getHost() + ":" + danmakuServer.getPort());
            MessageHandler.send(socket, Request.danmakuLogin(rid));
            logger.info("进入 " + rid + " 号房间， " + gid + " 号弹幕群组 ...");
            MessageHandler.send(socket, Request.joinGroup(rid, gid));

            //心跳包线程启动
            new Thread(new KeepLiveThread(socket), "KeepLive-").start();

            new Thread(new SendBarrage(socket), "sendBarrage").start();

            logger.info("开始接收弹幕 ...");
            logger.info("----------------------------------------------------------------");

//            DanmakuDao.createTable();

            MessageHandler.receive(socket, danmakuListener);


        } catch (IOException e) {
            e.printStackTrace();
            logger.error("Error" + e.toString());
            logger.error("与服务器连接失败!");
        }
    }

    /**
     * 判断当前房间是否在直播
     */
    private boolean isOnline() {
        String pageHtml = HttpRequest.get(roomUrl);
        return pageHtml != null && ResponseParser.parseOnline(pageHtml);
    }
}
