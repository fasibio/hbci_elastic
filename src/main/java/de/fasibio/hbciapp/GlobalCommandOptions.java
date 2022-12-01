package de.fasibio.hbciapp;

import picocli.CommandLine.Option;

public class GlobalCommandOptions {

  @Option(names = "--blz", description = "Bankleitzahl")
  static String blz;

  @Option(names = "--userid", description = "Bank online userid")
  static String userid;

  @Option(names = { "-p", "--password" }, description = "Passphrase", interactive = true)
  static char[] password;

}
