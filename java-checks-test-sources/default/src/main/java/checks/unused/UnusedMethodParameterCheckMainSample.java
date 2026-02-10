package checks.unused;

public class UnusedMethodParameterCheckMainSample {
  public static void main(String[] args) { // Noncompliant {{Remove this unused method parameter "args".}}
    System.out.println("Hello World!");
  }

  void main() { // Compliant
    System.out.println("Hello World!");
  }
}
