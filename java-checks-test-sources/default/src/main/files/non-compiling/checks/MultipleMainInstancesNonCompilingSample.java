class MultipleMainInstancesSample {
  public class UnknownSuper extends UndefinedParent {
    void main() { // Compliant - cannot verify if UndefinedParent has a main method
    }
  }

  // Decoy non-compliant example in the non-compiling test, so we can verifyIssues
  public static class NonCompliant {
    public static void main(String[] args) { // Noncompliant {{At most one main method should be defined in a class.}}
//                     ^^^^
      System.out.println("Static main detected; shadowing instance main.");
    }

    void main() {
      System.out.println("Unreachable entry point due to static precedence.");
    }
  }
}
