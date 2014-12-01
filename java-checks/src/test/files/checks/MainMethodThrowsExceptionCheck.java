import java.io.IOException;

class A{
  public static void main(String[] args) throws IOException { //NonCompliant
  }

  public void main(String[] args) throws IOException { //Compliant
  }

  public static void main(String[] args) { //Compliant
  }

}