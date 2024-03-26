package checks;

import javax.inject.Inject;

class ConstructorInjectionCheckSampleA {
   @Inject
   Object injectedfield; // Noncompliant
}

class ConstructorInjectionCheckSampleB {
  @Inject
  Object injectedField; // Compliant - private constructor 
  
  private ConstructorInjectionCheckSampleB() {}
  
}

class ConstructorInjectionCheckSampleC {
  @Inject
  Object injectedfield; // Noncompliant
  
  Object field; // compliant
  
  void foo() {}
  
  public ConstructorInjectionCheckSampleC() {}
}

class ConstructorInjectionCheckSampleD {
  Object field; // compliant
  
  void foo() {}
  
  public ConstructorInjectionCheckSampleD() {}
}
