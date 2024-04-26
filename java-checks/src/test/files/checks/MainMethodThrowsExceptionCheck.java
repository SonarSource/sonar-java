import java.io.IOException;

class A{
  public static void main(String[] args) throws IOException { // Noncompliant {{Remove this throws clause.}}
//                                       ^^^^^^
  }

  public void main(String[] args) throws IOException {
  }

  public static void main(String[] args) {
  }

}
