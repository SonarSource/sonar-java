package checks;

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
