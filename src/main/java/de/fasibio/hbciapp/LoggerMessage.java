package de.fasibio.hbciapp;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;

@JsonAutoDetect(fieldVisibility = Visibility.ANY)
public class LoggerMessage {
  String time;
  String[] data;
  String searchIndex;
  String type = "LOG";
  String static_content = "{}";
}
