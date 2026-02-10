package checks;

import java.io.IOException;

class MainMethodThrowsExceptionInstanceMainCheckSample {
  public void main(String[] args) throws IOException { // Noncompliant {{Remove this throws clause.}}
//                                ^^^^^^
  }

  public void main(int a, int b) throws IOException {}

  public void example() {}
}
