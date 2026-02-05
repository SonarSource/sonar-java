package checks.mainSignature;

public class PrivateMain {
  private static void main(String[] args) { // Noncompliant
  }

  private void main() { // Noncompliant
  }
}
