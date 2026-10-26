package checks;

interface InnerClassInInterfaceCheckSample {

  Result compute(Unknown unknown);

  Unknown.Result unknownQualified();

  class Result extends UnknownParent {} // Compliant - return type of an interface method

  class UnknownSuperclass extends UnknownParent implements InnerClassInInterfaceCheckSample { // Noncompliant
//      ^^^^^^^^^^^^^^^^^
  }

  class UnknownSuperinterface implements UnknownInterface {} // Noncompliant
//      ^^^^^^^^^^^^^^^^^^^^^
}
