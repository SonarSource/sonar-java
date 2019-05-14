import java.io.File;

class A {
  void foo() throws Exception {
    System.getenv(); // Noncompliant {{Make sure that environment variables are used safely here.}}
    System.getenv("HOME"); // Noncompliant

    ProcessBuilder processBuilder = new ProcessBuilder();
    Map<String, String> environment = processBuilder.environment(); // Noncompliant

    Runtime.getRuntime().exec("ping");
    Runtime.getRuntime().exec("ping", new String[]{"env=val"}, new File()); // Noncompliant
    Runtime.getRuntime().exec("ping", new String[]{"env=val"}); // Noncompliant
    Runtime.getRuntime().exec("ping", null);

    Runtime.getRuntime().exec(new String[]{"ping"});
    Runtime.getRuntime().exec(new String[]{"ping"}, new String[]{"env=val"}); // Noncompliant
    Runtime.getRuntime().exec(new String[]{"ping"}, new String[]{"env=val"}, new File()); // Noncompliant
    Runtime.getRuntime().exec(new String[]{"ping"}, null);

  }
}
