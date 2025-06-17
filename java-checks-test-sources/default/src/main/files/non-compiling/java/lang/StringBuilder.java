package java.lang;

public class StringBuilder {
  @Override
  public String toString() {
    return "";
  }

  public boolean isEmpty() {
    return true;
  }

  public void samples(){
    boolean b = toString().isEmpty(); // FN, we do not raise inside StringBuilder or StringBuffer
  }
}
