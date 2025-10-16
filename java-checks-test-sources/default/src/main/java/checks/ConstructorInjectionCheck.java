package checks;

import android.app.Activity;
import javax.inject.Inject;

class ConstructorInjectionCheckA {
   @Inject
   Object injectedfield; // Noncompliant
}

class ConstructorInjectionCheckB {
  @Inject
  Object injectedField; // Compliant - private constructor 
  
  private ConstructorInjectionCheckB() {}
  
}

class ConstructorInjectionCheckC {
  @Inject
  Object injectedfield; // Noncompliant
  
  Object field; // compliant
  
  void foo() {}
  
  public ConstructorInjectionCheckC() {}
}

class ConstructorInjectionCheckD {
  Object field; // compliant
  
  void foo() {}
  
  public ConstructorInjectionCheckD() {}
}

class AndroidActivityInjection extends Activity {
  @Inject // Compliant : Activities are managed by the framework, one cannot use constructor injection
  private String injected;
}
