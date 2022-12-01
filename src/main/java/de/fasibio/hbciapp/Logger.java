package de.fasibio.hbciapp;

import java.io.IOException;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;

import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class Logger {

  FunkAgent funkAgent = null;

  private Map<String, Object> globalValues = new HashMap<String, Object>();

  public Logger() {
    funkAgent = FunkAgent.getInstance();
  }

  public Logger addGlobalValues(Object... kv) {
    for (int i = 0; i < kv.length; i += 2) {
      globalValues.put((String) kv[i], kv[i + 1]);
    }
    return this;
  }

  /**
   * @return
   */
  public static Logger getLogger() {
    return new Logger();
  }

  public void info(String msg, Object... kv) {
    log(msg, LogLevel.INFO, kv);
  }

  public void debug(String msg, Object... kv) {
    log(msg, LogLevel.DEBUG, kv);
  }

  public void warn(String msg, Object... kv) {
    log(msg, LogLevel.WARN, kv);
  }

  public void log(String msg, LogLevel level, Object... kv) {
    Map<String, Object> map = new HashMap<String, Object>();

    map.put("@timestamp", Instant.now().toString());
    map.put("level", level.toString());
    map.put("message", msg);
    for (int i = 0; i < kv.length; i += 2) {
      map.put((String) kv[i], kv[i + 1]);
    }
    map.putAll(globalValues);
    ObjectMapper objectMapper = new ObjectMapper();
    try {
      String json = objectMapper.writeValueAsString(map);
      LoggerMessage funk_msg = new LoggerMessage();
      funk_msg.time = Instant.now().toString();
      funk_msg.data = new String[] { json };
      funk_msg.searchIndex = "bank";
      TextMessage strMsg = new TextMessage(new ObjectMapper().writeValueAsString(new LoggerMessage[] { funk_msg }));
      funkAgent.webSocketSession.sendMessage(strMsg);
      System.out.println(json);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
