package de.fasibio.hbciapp;

public enum LogLevel {
  INFO("info"), DEBUG("debug"), WARN("warn");

  private final String text;

  LogLevel(final String text) {
    this.text = text;
  }

  @Override
  public String toString() {
    return text;
  }
}
