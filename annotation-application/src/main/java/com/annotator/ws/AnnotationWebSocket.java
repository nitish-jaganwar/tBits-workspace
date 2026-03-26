package com.annotator.ws;


import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;

@ServerEndpoint("/ws-annotator/{docId}")
public class AnnotationWebSocket {

    private static Set<Session> userSessions = Collections.synchronizedSet(new HashSet<>());

    @OnOpen
    public void onOpen(Session session, @PathParam("docId") String docId) {
        System.out.println("New WebSocket connection for Doc: " + docId + ", Session ID: " + session.getId());
        userSessions.add(session);
    }

    @OnMessage
    public void onMessage(String message, Session userSession) {
        System.out.println("Message Received: " + message);
        // Broadcast message to all other connected clients @users
        for (Session session : userSessions) {
            if (session.isOpen() && !session.getId().equals(userSession.getId())) {
                session.getAsyncRemote().sendText(message);
            }
        }
    }

    @OnClose
    public void onClose(Session session) {
        System.out.println("Connection closed: " + session.getId());
        userSessions.remove(session);
    }

    @OnError
    public void onError(Session session, Throwable throwable) {
        System.err.println("WebSocket Error on session " + session.getId() + ": " + throwable.getMessage());
    }
}