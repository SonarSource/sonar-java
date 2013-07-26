class Foo {
  public void myMethod() {              // Compliant
    if(something)
    {                                   // Non-Compliant
      executeTask();
    } else {                            // Compliant
      doSomethingElse();
    }
    if( param1 && param2 && param3
      && something3 && something4)
    {                                   // Non-Compliant
      executeAnotherTask();
    }
  }

  {                                     // Compliant
    System.out.println("static intializer");
  }

  public void foo() {
    {                                   // Compliant
    }
  }
}

@Properties(
{ // Compliant
})
class Exceptions {
  int[] numbers = new int[]
{ 0, 1 }; // Compliant
}
