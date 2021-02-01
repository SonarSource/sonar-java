package checks;

import java.util.List;

class OSCommandsPath {
  private void processBuilderListJava9() {
    new ProcessBuilder(List.of("make"));  // Noncompliant {{Make sure the "PATH" used to find this command includes only what you intend.}}
    new ProcessBuilder(List.of("m../ake"));   // Noncompliant {{Make sure the "PATH" used to find this command includes only what you intend.}}
    new ProcessBuilder(List.of("mak./e"));   // Noncompliant {{Make sure the "PATH" used to find this command includes only what you intend.}}
    new ProcessBuilder(List.of("bin~/make"));   // Noncompliant {{Make sure the "PATH" used to find this command includes only what you intend.}}
    new ProcessBuilder(List.of("7:\\\\make"));  // Noncompliant {{Make sure the "PATH" used to find this command includes only what you intend.}}
    new ProcessBuilder(List.of("m..\\ake"));  // Noncompliant {{Make sure the "PATH" used to find this command includes only what you intend.}}
    new ProcessBuilder(List.of("ma.\\ke"));  // Noncompliant {{Make sure the "PATH" used to find this command includes only what you intend.}}
    new ProcessBuilder(List.of("SERVER\\make"));  // Noncompliant {{Make sure the "PATH" used to find this command includes only what you intend.}}
    new ProcessBuilder(List.of("/usr/bin/make"));
    new ProcessBuilder(List.of("../make"));
    new ProcessBuilder(List.of("./make"));
    new ProcessBuilder(List.of("~/bin/make"));
    new ProcessBuilder(List.of("Z:\\make"));
    new ProcessBuilder(List.of("..\\make"));
    new ProcessBuilder(List.of(".\\make"));
    new ProcessBuilder(List.of("\\\\SERVER\\make"));
  }

  private void commandJava9() {
    ProcessBuilder builder = new ProcessBuilder();

    builder.command(List.of("make"));  // Noncompliant {{Make sure the "PATH" used to find this command includes only what you intend.}}
    builder.command(List.of("m../ake"));   // Noncompliant {{Make sure the "PATH" used to find this command includes only what you intend.}}
    builder.command(List.of("mak./e"));   // Noncompliant {{Make sure the "PATH" used to find this command includes only what you intend.}}
    builder.command(List.of("bin~/make"));   // Noncompliant {{Make sure the "PATH" used to find this command includes only what you intend.}}
    builder.command(List.of("7:\\\\make"));  // Noncompliant {{Make sure the "PATH" used to find this command includes only what you intend.}}
    builder.command(List.of("m..\\ake"));  // Noncompliant {{Make sure the "PATH" used to find this command includes only what you intend.}}
    builder.command(List.of("ma.\\ke"));  // Noncompliant {{Make sure the "PATH" used to find this command includes only what you intend.}}
    builder.command(List.of("SERVER\\make"));  // Noncompliant {{Make sure the "PATH" used to find this command includes only what you intend.}}
    builder.command(List.of("/usr/bin/make"));
    builder.command(List.of("../make"));
    builder.command(List.of("./make"));
    builder.command(List.of("~/bin/make"));
    builder.command(List.of("Z:\\make"));
    builder.command(List.of("..\\make"));
    builder.command(List.of(".\\make"));
    builder.command(List.of("\\\\SERVER\\make"));
  }
}
