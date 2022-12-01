package de.fasibio.hbciapp;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.ExitCodeGenerator;
import org.springframework.stereotype.Component;

import picocli.CommandLine;
import picocli.CommandLine.IFactory;
import picocli.shell.jline3.PicocliCommands;

@Component
public class MyApplicationRunner implements CommandLineRunner, ExitCodeGenerator {

  private final Command myCommand;

  private final IFactory factory; // auto-configured to inject PicocliSpringFactory

  private int exitCode;

  public MyApplicationRunner(Command myCommand, IFactory factory) {
    this.myCommand = myCommand;
    this.factory = factory;
  }

  @Override
  public void run(String... args) throws Exception {
    CommandLine cmd = new CommandLine(myCommand, factory);
    exitCode = cmd.execute(args);
  }

  @Override
  public int getExitCode() {
    return exitCode;
  }
}