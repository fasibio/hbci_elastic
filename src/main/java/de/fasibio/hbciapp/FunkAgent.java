package de.fasibio.hbciapp;

import java.net.URI;
import java.util.concurrent.ExecutionException;

import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.WebSocketClient;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.TextWebSocketHandler;

public class FunkAgent {
  private static FunkAgent instance = null;
  WebSocketClient client = new StandardWebSocketClient();
  WebSocketSession webSocketSession;

  private FunkAgent(String url, String connectionKey) {

    WebSocketHttpHeaders headers = new WebSocketHttpHeaders();
    headers.add("funk.connection", connectionKey);

    try {
      webSocketSession = client.execute(new TextWebSocketHandler() {
        @Override
        public void handleTextMessage(WebSocketSession session, TextMessage message) {
          System.out.println("received message - " + message.getPayload());
        }

        @Override
        public void afterConnectionEstablished(WebSocketSession session) {
          System.out.println("established connection - " + session);
        }
      }, headers, URI.create(url)).get();
    } catch (InterruptedException | ExecutionException e) {
      e.printStackTrace();
    }
  }

  public static FunkAgent initInstance(String url, String connectionKey) {
    if (instance == null) {
      instance = new FunkAgent(url, connectionKey);
    }
    return instance;
  }

  public static FunkAgent getInstance() {

    return instance;
  }

}
