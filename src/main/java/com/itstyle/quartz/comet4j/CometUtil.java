package com.itstyle.quartz.comet4j;

import com.itstyle.quartz.comet4j.constant.Constant;
import org.comet4j.core.CometConnection;
import org.comet4j.core.CometContext;
import org.comet4j.core.CometEngine;
import org.comet4j.core.event.ConnectEvent;
import org.comet4j.core.listener.ConnectListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

//@WebListener
public class CometUtil extends ConnectListener implements ServletContextListener {
    private final static Logger LOGGER = LoggerFactory.getLogger(CometUtil.class);
    /**
     * 初始化上下文
     */
    @Override
    public void contextInitialized(ServletContextEvent arg0) {
        // CometContext ： Comet4J上下文，负责初始化配置、引擎对象、连接器对象、消息缓存等。
        CometContext cc = CometContext.getInstance();
        // 注册频道，即标识哪些字段可用当成频道，用来作为向前台传送数据的“通道”
        cc.registChannel(Constant.CHANNEL_MSGCOUNT);
        cc.registChannel(Constant.CHANNEL_MSG_DATA);
        //添加监听器
        CometEngine engine = CometContext.getInstance().getEngine();
        engine.addConnectListener(this);
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        // TODO Auto-generated method stub
    }

    @Override
    public boolean handleEvent(ConnectEvent connEvent) {

        final CometConnection conn = connEvent.getConn();
        HttpServletRequest request = conn.getRequest();
        try {
            HttpSession session = request.getSession();
            Object userId = session.getAttribute("currentUserId");
            CacheManager.putContent(userId.toString(), connEvent);
        } catch (Exception e) {
            LOGGER.error("发生异常！msg={}", e.getMessage());
          // e.printStackTrace();

        }
        return true;
    }

    private void doCache(final CometConnection conn, String userId) {
        if (userId != null) {
            CacheManager.putContent(conn.getId(), String.valueOf(userId), Constant.EXPIRE_AFTER_ONE_HOUR);
        }
    }

    /**
     * 推送给所有的客户端
     *
     * @param comet
     */
    public void pushToAll(Comet comet) {
        try {
            CometEngine engine = CometContext.getInstance().getEngine();
            //推送到所有客户端
            engine.sendToAll(Constant.CHANNEL_MSGCOUNT, comet.getMsgCount());
            engine.sendToAll(Constant.CHANNEL_MSG_DATA, comet.getMsgData());
        } catch (Exception e) {
            // TODO: handle exception
            System.out.println(e.getMessage());
        }

    }

    /**
     * 推送给指定客户端
     *
     * @param comet
     */
    public void pushTo(Comet comet) {
        try {
            ConnectEvent connEvent = (ConnectEvent) CacheManager.getContent(comet.getUserId()).getValue();
            final CometConnection conn = connEvent.getConn();
            //建立连接和用户的关系
            doCache(conn, comet.getUserId());
            final String connId = conn.getId();
            CometEngine engine = CometContext.getInstance().getEngine();
            if (CacheManager.getContent(connId).isExpired()) {
                doCache(conn, comet.getUserId());
            }
            //推送到指定的客户端
            engine.sendTo(Constant.CHANNEL_MSGCOUNT, engine.getConnection(connId), comet.getMsgCount());
            engine.sendTo(Constant.CHANNEL_MSG_DATA, engine.getConnection(connId), comet.getMsgData());
        } catch (Exception e) {
            // TODO: handle exception
            System.out.println(e.getMessage());
        }
    }
}