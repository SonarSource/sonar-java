package java.lang;

public class IsInstanceMethodCheckSample {
 
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
    
    
    XXXX.class.isInstance(o);
    XXXX[].class.isInstance(o);
    com.example.xxx.XXXX[].class.isInstance(o);
  }
}
