package checks;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

class MyTestClass {}

interface Interface {}

class Clazz implements Interface {}

public class VarCanBeUsedCheck {

  public static final MyTestClass MyTest = new MyTestClass(); // Compliant
  
  void f(int[] array) {
    String undefinedString;
    
    String s = "ABC"; // Noncompliant
    String s1 = new String("ABC"); // Noncompliant [[sc=12;ec=14]] {{Declare this local variable with "var" instead.}}
    int i = 10; // Noncompliant
    long l = 10L; // Noncompliant

    ArrayList<String> arrayList = new ArrayList<>(5); // Compliant
    List<String> list = new ArrayList<>(5); // Compliant

    int sum = list.stream().mapToInt(String::length).sum(); // Compliant, too many calls

    String format = String.format("%s %s %s", "a", "b", "c"); // Noncompliant
    
    Object o = new Object(); // Noncompliant
    Object ooo = getObject(); // Noncompliant
    Object oooo = getO(); // Compliant, not obvious from a method name

    Interface inter = new Clazz(); // Compliant
    
    var arrayList1 = new ArrayList<>(); // Compliant

    var varObject = getObject(); // Compliant
    
    var sss = "STRING"; // Compliant

    for (int j = 0, length = array.length; j <= length; j++) { // Compliant, 'var' not allowed here
    }

    for (int index = 0; index <= array.length; index++) { // Noncompliant
    }
    
    for (var j = 0; j <= array.length; j++) { // Compliant
    }
    
    int length = "AbCDE".length(); // Compliant, primitive not initialised with a literal
    
    var length1 = "AbCDE".length(); // Compliant
    
    long u = System.currentTimeMillis(), uu = System.nanoTime(); // Compliant, 'var' not allowed here

    long u1 = System.currentTimeMillis(), // Compliant, 'var' not allowed here
      uu1 = System.nanoTime(); // Compliant, 'var' not allowed here

    char c = i == 3 ? 0 : "hdr".charAt(i); // Compliant, despite it's possible here, ternary  operator may bring confusion in understanding the inferred type

    byte[] input = new byte[array.length]; // Noncompliant
    
    var input2 = new byte[array.length]; // Compliant

    Abc abc = Abc.getAbc(); // Noncompliant

    Abc abavababab = Abc.getAbc(); // Noncompliant

    var varAbc = Abc.getAbc(); // Compliant


    Abc myCustom = varAbc; // Noncompliant
  }

  void testFile(File file) {
    try (FileInputStream in = new FileInputStream(file)) // Noncompliant
    {
      in.read();
    }
    catch (IOException e)
    {
    }

    try (var in = new FileInputStream(file)) // Compliant
    {
      in.read();
    }
    catch (IOException e)
    {
    }
  }

  private Object getObject() {
    return new Object();
  }

  private Object getO() {
    return new Object();
  }
}


class Abc {
  static Abc getAbc() {
    return new Abc();
  }
}
