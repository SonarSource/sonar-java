package org.sonar.java.checks.security;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;

public class Foo {
  public void runtime() throws IOException {
    Runtime r = Runtime.getRuntime();
    r.exec("mv a/ b/"); // Noncompliant {{Make sure that executing this OS command is safe here.}}

    r.exec("cd ../a", new String[] {"foo=bar"}); // Noncompliant [[sc=5;ec=48]]
    r.exec("cd ../a", new String[] {"foo=bar"}, new File("../c")); // Noncompliant
    r.exec(new String[] {"cd ../a"}); // Noncompliant
    r.exec(new String[] {"cd ../a"}, new String[] {"foo=bar"}); // Noncompliant
    r.exec(new String[] {"cd ../a"}, new String[] {"foo=bar"}, new File("../c")); // Noncompliant
  }

  public void commandLine() throws IOException {
    String line = "bad.exe";
    CommandLine cmdLine = CommandLine.parse("bad.exe");
    DefaultExecutor executor = new DefaultExecutor();
    executor.execute(cmdLine); // Noncompliant
  }

  public void processBuilder() {
    List<String> commands = Arrays.asList("myCommand", "myArg1", "myArg2");
    ProcessBuilder pb =
      new ProcessBuilder("myCommand", "myArg1", "myArg2"); // Noncompliant [[sc=7;ec=58]]
    ProcessBuilder pb2 =
      new ProcessBuilder(commands); // Noncompliant
    pb.command();
    pb.command(commands); // Noncompliant
    pb.command("myCommand", "myArg1", "myArg2"); // Noncompliant

    ProcessBuilder pb3 = new ProcessBuilder();
  }
}
