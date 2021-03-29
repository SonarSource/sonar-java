package java.lang;

public class IsInstanceMethodCheck {
 
  int compliant(Number n) {
    if (n instanceof String) {  // Compile-time error
      return 42;
    }
    return 0;
  }
  
}

class Class {
  
  boolean isInstance(Object o) {
    return true;
  }
  
  void function(Object o) {
    boolean instance = isInstance(o);
    boolean instance2 = this.isInstance(o);
  }
}
