package checks;

import java.util.List;

class OSCommandsPath {
  private static final String UNRESOLVABLE_WITH_INDIRECTION = NonExisting.UNRESOLVABLE;

  private static final String UNINITIALIZED_COMMAND;
  private static final String[] UNINITIALIZED_COMMAND_ARRAY;
  private static final List<String> UNINITIALIZED_COMMAND_LIST;

  private void processUnresolvable() {
    new ProcessBuilder(UNRESOLVABLE);
    new ProcessBuilder(UNRESOLVABLE_WITH_INDIRECTION);
    new ProcessBuilder(NonExisting.UNRESOLVABLE);
  }

  private void noInitializer() {
    Runtime.getRuntime().exec(UNINITIALIZED_COMMAND);
    Runtime.getRuntime().exec(UNINITIALIZED_COMMAND_ARRAY);
    new ProcessBuilder(UNINITIALIZED_COMMAND);
    new ProcessBuilder(UNINITIALIZED_COMMAND_LIST);
    new ProcessBuilder().command(UNINITIALIZED_COMMAND);
    new ProcessBuilder().command(UNINITIALIZED_COMMAND_ARRAY);
    new ProcessBuilder().command(UNINITIALIZED_COMMAND_LIST);
  }

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
