class Foo {    // Noncompliant {{Move this left curly brace to the beginning of next line of code.}}
}

class Foo
{              // Compliant
}

class
Foo {          // Noncompliant {{Move this left curly brace to the beginning of next line of code.}}
}

class Foo
{              // Compliant
  void foo() { // Noncompliant {{Move this left curly brace to the beginning of next line of code.}}
  }
}

class Foo
{              // Compliant
  void foo()
  {            // Compliant
  }
}

@Properties({ // Compliant
})
class Exceptions
{
  int[] numbers = new int[] { 0, 1 }; // Compliant
}
public @interface Resolver
{
  String codeRuleId();
}