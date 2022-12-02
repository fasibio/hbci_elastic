package de.fasibio.hbciapp;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentMap;

import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.springframework.stereotype.Component;

import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

@Component
@picocli.CommandLine.Command(name = "hbci", mixinStandardHelpOptions = true, subcommands = { Command.Init.class,
    Command.ListKonto.class })
public class Command implements Callable<Integer> {

  @Mixin
  GlobalCommandOptions globalCommands = new GlobalCommandOptions();

  @Override
  public Integer call() throws Exception {
    return 302;
  }

  @Component
  @picocli.CommandLine.Command(name = "load", mixinStandardHelpOptions = true)
  static class Init implements Callable<Integer> {

    @Mixin
    GlobalCommandOptions globalCommands = new GlobalCommandOptions();

    @Option(names = { "--konto", "-k" }, description = "Bank online userid")
    static String konto;

    @Option(names = { "--fake-data" }, description = "Use Fake Saldo (Presentation Mode)")
    static boolean fakeData;
    @Option(names = {
        "--funk_connectionKey" }, description = "Shared Secret to connect to FunkServer", defaultValue = "changeMe7894561323")
    static String funk_ConnectionKey;

    @Option(names = {
        "--funk_url" }, description = " FunkServer Websocket url", defaultValue = "ws://localhost:3000/data/subscribe")
    static String funk_url;

    @Override
    public Integer call() throws Exception {
      Files.createDirectories(Paths.get("./db"));

      DB db = DBMaker.fileDB("./db/loadInfo.db").make();
      try {

        ConcurrentMap map = db.hashMap("databasereaer").createOrOpen();
        String DbKey = this.globalCommands.blz + "_" + this.globalCommands.userid + "_" + this.konto;

        FunkAgent.initInstance(funk_url, funk_ConnectionKey);
        UmsatzAbrufPinTan umsatzCaller = new UmsatzAbrufPinTan(this.globalCommands.blz, this.globalCommands.userid,
            new String(this.globalCommands.password));

        String lastReadDate = (String) map.get(DbKey);
        if (lastReadDate == null) {
          umsatzCaller.collect(this.konto, fakeData);
        } else {
          umsatzCaller.collect(this.konto, Date.from(Instant.parse(lastReadDate)), fakeData);
        }
        map.put(DbKey, Instant.now().toString());
      } finally {
        File f = new File("./testpassport.dat");
        f.deleteOnExit();
        db.close();
      }
      return 200;
    }

  }

  @Component
  @picocli.CommandLine.Command(name = "ls", mixinStandardHelpOptions = true)
  static class ListKonto implements Callable<Integer> {

    @Mixin
    GlobalCommandOptions globalCommands = new GlobalCommandOptions();

    @Override
    public Integer call() throws Exception {

      UmsatzAbrufPinTan umsatzCaller = new UmsatzAbrufPinTan(this.globalCommands.blz, this.globalCommands.userid,
          new String(this.globalCommands.password));

      List<String> kNumber = umsatzCaller.listKontos();
      for (String number : kNumber) {
        System.out.println(number);
      }
      return 200;
    }

  }

}
