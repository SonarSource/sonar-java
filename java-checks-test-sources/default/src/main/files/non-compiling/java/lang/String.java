package java.lang;

public class String {

  public boolean isEmpty() {
    return true;
  }

  int length() {
    return 0;
  }

  public void samples(){
    boolean b = length() > 0; // Noncompliant
  }
}

