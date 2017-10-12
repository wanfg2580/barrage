package com.jimmy;

import com.jimmy.bean.Request;
import com.jimmy.handler.MessageHandler;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.net.Socket;

/**
 * Created by Brucezz on 2016/01/04.
 * DouyuCrawler
 */
public class KeepLiveThread implements Runnable {
    private static Logger logger = Logger.getLogger(KeepLiveThread.class);
    private Socket s;

    public KeepLiveThread(Socket s) {
        this.s = s;
    }

    @Override
    public void run() {

        logger.info("KeepLive" + "心跳包线程启动 ...");

        while (s != null && s.isConnected()) {
            try {
                Thread.sleep(40000);
                MessageHandler.send(s, Request.keepLive((int) (System.currentTimeMillis() / 1000)));
                logger.debug("KeepLive Keep Live ...");
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}