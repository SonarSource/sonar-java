class TooManyStatementsPerLine {
  int a; int b; // OK - not a statement

  void method() {
    doSomething(); doSomethingElse(); // NOK

    if (a) {} // OK

    if (a) {} if (b) {} // NOK

    while (condition); // OK

    label: while (condition) { // OK
      break label; // OK
    }

    int a = 0; a++; // NOK
    doSomething(
    ); doSomethingElse(); // NOK
    if (a)
      System.out.println("plop"); //OK
    try {
      System.out.println("plop"); //OK
    } catch (Exception e){
      return ""; } //OK
    if (a) {
      return doSomething(
      );} //OK
    if (a)
      return "";
    else if (b)
      return "";
    else
    {
      return "";
    } //OK
    if(a) return false;
    if (debug) System.out.println(
        "ServerTableEntry constructed with activation command " +
            activationCmd);
    if(a){
    }else if(b) return true;
  }
}
