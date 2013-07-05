class Foo {    // Non-Compliant
}

class Foo
{              // Compliant
}

class
Foo {          // Non-Compliant
}

class Foo
{              // Compliant
  void foo() { // Non-Compliant
  }
}

class Foo
{              // Compliant
  void foo()
  {            // Non-Compliant
  }
}
