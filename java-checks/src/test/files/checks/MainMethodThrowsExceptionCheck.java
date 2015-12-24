import java.io.IOException;

class A{
  public static void main(String[] args) throws IOException { // Noncompliant [[sc=42;ec=48]] {{Remove this throws clause.}}
  }

  public void main(String[] args) throws IOException {
  }

  public static void main(String[] args) {
  }

}
