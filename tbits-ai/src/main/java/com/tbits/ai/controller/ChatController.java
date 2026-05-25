package com.tbits.ai.controller;

import com.tbits.ai.service.RAGService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.logging.Logger;

@RestController
@RequestMapping("/api/chat")
@CrossOrigin("*")
public class ChatController {

	private static final Logger logger = Logger.getLogger(ChatController.class.getName());

	@Autowired
	private RAGService ragService;

	@PostMapping
	public ResponseEntity<String> chat(@RequestBody String userMessage) {

		logger.info("👤 User Query: " + userMessage);

		try {

			String response = ragService.getAnswer(userMessage);

			return ResponseEntity.ok(response);

		} catch (Exception e) {

			logger.severe("🚨 Error: " + e.getMessage());

			return ResponseEntity.internalServerError().body("System Error");
		}
	}
}
