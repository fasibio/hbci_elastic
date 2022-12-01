package de.fasibio.hbciapp;

import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;

@ShellComponent
public class ShellRoot {

  @ShellMethod(value = "add keys")
  public void sshAdd(@ShellOption(value = "--k", arity = 2) String[] keys) {
    System.out.println("blubb");
  }

}
