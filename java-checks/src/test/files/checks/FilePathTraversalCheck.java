import java.io.File;

class A {
  
  String myField;
  
  public void foo(File parent, String arg0, String arg1, String arg2, String arg3, String arg4, String arg5, String arg6, String arg7, String arg8) {
    new File(arg0); // Noncompliant {{"arg0" is provided externally to the method and not sanitized before use.}}
    new File(arg0); // Noncompliant {{"arg0" is provided externally to the method and not sanitized before use.}}
    
    new File(arg1); // Noncompliant {{"arg1" is provided externally to the method and not sanitized before use.}}
    foo(arg1);
    new File(arg1); // Compliant
    
    foo(arg2);
    new File(arg2); // Compliant
    
    new File(parent, arg3); // Noncompliant {{"arg3" is provided externally to the method and not sanitized before use.}}
    
    if (!arg4.isEmpty()) {
      new File(arg4, arg5); // Noncompliant {{"arg5" is provided externally to the method and not sanitized before use.}}
    }
    
    new File("tmp"); // Compliant
    
    new File(new File(arg6), arg4); // Noncompliant  {{"arg6" is provided externally to the method and not sanitized before use.}}
    
    new File(arg7.replaceAll("[./\\]","")); // Compliant
    
    new File(foo(arg8)); // Compliant
    
    new File(myField); // Compliant
  }
  
  String foo(String s) {
    new A();
    return s; 
  }
}
