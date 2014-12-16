class A {
  public void listContent(String input) {
    Runtime rt = Runtime.getRuntime();
    String[] cmds;
    rt.exec("ls " + input); // Noncompliant; input could easily contain extra commands
    rt.exec(cmds); // Noncompliant
    rt.exec(new String[]{" ", input}); // Noncompliant
    rt.exec(new String[]{" ", " "}); // Compliant
  }

  public void execute(String command, String argument) {
    ProcessBuilder pb = new ProcessBuilder(command, argument); // Noncompliant
    pb = new ProcessBuilder(command, argument); // Noncompliant
    pb = new ProcessBuilder("", argument); // Compliant
    pb = new ProcessBuilder(new String[]{" ", command}, argument); // Noncompliant
  }
}