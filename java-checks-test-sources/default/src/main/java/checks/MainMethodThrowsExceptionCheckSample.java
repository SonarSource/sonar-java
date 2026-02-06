package checks;

import java.io.IOException;

class MainMethodThrowsExceptionCheckSample {
  static class Bad {
    public static void main(String[] args) throws IOException { // Noncompliant {{Remove this throws clause.}}
//                                         ^^^^^^
    }
  }

  //  Before Java 25, this is not a program entry point.
  static class InstanceMain {
    public void main(String[] args) throws IOException {
    }
  }

  static class Good {
    public static void main(String[] args) {
    }
  }
}
