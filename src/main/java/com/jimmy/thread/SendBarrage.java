package com.jimmy.thread;

import com.jimmy.bean.Request;
import com.jimmy.handler.MessageHandler;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import java.io.IOException;
import java.net.Socket;
import java.util.Date;

/**
 *
 * @author jimmy
 * @desc SendBarrage
 * @date 2017/10/12
 */
public class SendBarrage implements Runnable {

    private Date date;

    private Socket socket;

    public SendBarrage(Socket socket) {
        this.socket = socket;
    }

    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        Date date = new Date();
        String barrage = "#签到 " + date.toString();
//        MessageHandler.send(s, Request.buildMessage(barrage));
    }

    @Override public void run() {
        Date date = new Date();
        String barrage = "#签到 " + date.toString();
        String s = Request.buildMessage("74751", "3350664", barrage, 15);
        try {
            MessageHandler.send(socket, s);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
