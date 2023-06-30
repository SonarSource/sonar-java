package checks;

import java.util.stream.Collectors;
import java.util.stream.Stream;

class IssueClass { // Noncompliant {{This class is package private but is never used within the package}}
  Boolean t; // Noncompliant {{This variable is package private but is never used within the package}}

  void foo(Stream<NonIssueClass> stream) { // Noncompliant {{This method is package private but is never used within the package}}
    stream.map(NonIssueClass::nonIssueMethod3)
      .collect(Collectors.toList());

    var x = NonIssueClass.nonIssueVar;
    NonIssueClass.nonIssueMethod();
    var c = new NonIssueClass();
    c.nonIssueMethod2();
    var a = new NonIssueClass2[] {};
    var b = new java.util.ArrayList<NonIssueClass3>();

  }

  private Interface inlineImpl = 
    new Interface() {  // Noncompliant TODO: False positive
      @Override
      public void method() { }
  };

  private Interface lambda = () -> {};
  
}

interface Interface {
  void method();
}

class B{
  IssueClass x() {return null;}
}
