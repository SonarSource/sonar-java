import java.io.BufferedReader;
import java.io.Console;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Scanner;
import java.util.function.Consumer;

import static java.lang.System.in;

abstract static class A {

  @FunctionalInterface
  public interface ClosableFunctionalInterface {
    void close() throws IOException;
  }

  private InputStream in;

  public void foo() throws IOException {
    System.in.read(); // Noncompliant
    doSomething(System.in); // Noncompliant
    System.out.println("");
    System.in.close();
    (System.in).close();
    doAnotherThing(System.in::close);
    this.in.read();

    InputStream varIn = System.in; // Noncompliant
    varIn.read(); // Ok- reference above already raised issue

    Scanner scanner = new Scanner(System.in); // Noncompliant
    int i = scanner.nextInt();

    BufferedReader br = new BufferedReader(new InputStreamReader(System.in)); // Noncompliant

    System.setIn(new FileInputStream("test.txt")); // Noncompliant
    doSomethingElse(System::setIn); // Noncompliant
  }

  public void bar() throws IOException {
    Console console = System.console();
    console.reader(); // Noncompliant
    console.readLine(); // Noncompliant
    System.console().readLine("Prompt message %s", "arg"); // Noncompliant
    System.console().readPassword(); // Noncompliant
    console.readPassword("Prompt message %s %.2f", "arg", 1); // Noncompliant

    System.console().printf("printSomething");
    console.writer();
  }

  abstract void doSomething(java.io.InputStream inputStream);

  abstract void doSomethingElse(Consumer<InputStream> methodReference);

  abstract void doAnotherThing(ClosableFunctionalInterface customInterface);

}

abstract static class B {
  public void foo() throws IOException {
    in.read(); // Noncompliant
    in.close();
    doSomething(in); // Noncompliant
  }

  abstract void doSomething(java.io.InputStream inputStream);

}
