package checks;

import java.util.ArrayList;
import java.util.List;

class MyTestClass {}

public class VarCanBeUsedCheck {

  public static final MyTestClass MyTest = new MyTestClass(); // Compliant
  
  
  
  void f(int[] array) {
    String undefinedString;
    
    String s = "ABC"; // Noncompliant
    String s1 = new String("ABC"); // Noncompliant
    int i = 10; // Noncompliant
    long l = 10L; // Noncompliant

    ArrayList<String> arrayList = new ArrayList<>(5); // Noncompliant
    List<String> list = new ArrayList<>(5); // Compliant
    
    String format = String.format("%s %s %s", "a", "b", "c"); // Noncompliant
    
    
    Object o = new Object(); // Noncompliant
    Object ooo = getObject(); // Noncompliant
    
    
    var varObject = getObject(); // Compliant
    
    var sss = "STRING"; // Compliant


    for (int j = 0, length = array.length; j <= length; j++) { // Compliant, 'var' not allowed here
    }

    for (int index = 0; index <= array.length; index++) { // Noncompliant
    }
    
    for (var j = 0; j <= array.length; j++) { // Compliant
    }

    char c = i == 3 ? 0 : "hdr".charAt(i); // Compliant, despite it's possible here, ternary  operator may bring confusion in understanding the inferred type

    byte[] input = new byte[array.length]; // Noncompliant
    
    var input2 = new byte[array.length]; // Compliant
  }
  
  
  
  private Object getObject() {
    return new Object();
  }
}
