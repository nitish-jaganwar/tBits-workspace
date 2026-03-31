package com.annotator.ws;

import java.io.IOException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.websocket.CloseReason;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;

@ServerEndpoint("/ws-annotator/{documentId}")
public class AnnotationWebSocket {

	private static final Map<String, Set<Session>> documentRooms = new ConcurrentHashMap<>();

	// FIX : Safe token extraction using URLDecoder
	// Java 8 compatible — pass charset name as String, handle the checked exception
	private String extractToken(String queryString) {
		if (queryString == null || queryString.isEmpty())
			return null;
		for (String param : queryString.split("&")) {
			String[] pair = param.split("=", 2);
			if (pair.length == 2 && "token".equals(pair[0])) {
				try {
					return URLDecoder.decode(pair[1], "UTF-8");
				} catch (java.io.UnsupportedEncodingException e) {
					// UTF-8 is always guaranteed present — this branch is unreachable in practice
					return pair[1]; // fall back to raw value
				}
			}
		}
		return null;
	}

	@OnOpen
	public void onOpen(Session session, @PathParam("documentId") String documentId) {
		String token = extractToken(session.getQueryString());

		if (token == null || token.isEmpty()) {
			System.err.println("❌ Rejected: No Token");
			try {
				session.close(new CloseReason(CloseReason.CloseCodes.VIOLATED_POLICY, "Unauthorized"));
			} catch (IOException ignored) {
			}
			return;
		}

		session.getUserProperties().put("documentId", documentId);
		documentRooms.computeIfAbsent(documentId, k -> Collections.synchronizedSet(new HashSet<>())).add(session);

		System.out.println("✅ Connected to room: " + documentId);
	}

	@OnMessage
	public void onMessage(String message, Session sender, @PathParam("documentId") String documentId) {
		Set<Session> roomSessions = documentRooms.get(documentId);
		if (roomSessions == null)
			return;

		List<Session> toRemove = new ArrayList<>();

		synchronized (roomSessions) {
			for (Session s : roomSessions) {
				if (!s.isOpen()) {
					toRemove.add(s); // FIX : collect dead sessions
					continue;
				}
				if (s.getId().equals(sender.getId()))
					continue;

				// FIX : handle async send failures
				s.getAsyncRemote().sendText(message, result -> {
					if (!result.isOK()) {
						System.err
								.println("⚠️ Send failed to " + s.getId() + ": " + result.getException().getMessage());
						cleanupSession(s);
					}
				});
			}
			roomSessions.removeAll(toRemove); // FIX : purge dead sessions
		}

		pruneRoomIfEmpty(documentId, roomSessions);
	}

	@OnClose
	public void onClose(Session session) {
		cleanupSession(session);
	}

	@OnError
	public void onError(Session session, Throwable throwable) {
		System.err.println("❌ Error on " + session.getId() + ": " + throwable.getMessage());
		cleanupSession(session); // FIX : always clean up on error
	}

	// FIX : shared cleanup used by onClose AND onError
	private void cleanupSession(Session session) {
		String documentId = (String) session.getUserProperties().get("documentId");
		if (documentId == null)
			return;

		Set<Session> roomSessions = documentRooms.get(documentId);
		if (roomSessions != null) {
			roomSessions.remove(session);
			pruneRoomIfEmpty(documentId, roomSessions);
		}

		System.out.println(" Disconnected from room: " + documentId);
	}

	// FIX : atomic empty-room check using computeIfPresent
	private void pruneRoomIfEmpty(String documentId, Set<Session> roomSessions) {
		documentRooms.computeIfPresent(documentId, (k, sessions) -> sessions.isEmpty() ? null : sessions // returning
																											// null
																											// removes
																											// the key
																											// atomically
		);
	}
}