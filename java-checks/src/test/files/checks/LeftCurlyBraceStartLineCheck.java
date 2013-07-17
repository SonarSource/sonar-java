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
  {            // Compliant
  }
}

@Properties({ // Compliant
})
class Exceptions
{
  int[] numbers = new int[] { 0, 1 }; // Compliant
}
