public class HelloWorld {

  public HelloWorld() {
    super(); // +1
  }

  {
    int j = 0; //+1 for local variable in block
  }

  static {
    int y = 0; //+1 for local variable in block
  }

  public void sayHello() {
    int localVar; // +1 local-variable-declaration-statement
    localVar = 42; // +1 expression-statement

    assert true : "not true"; // +1 assert-statement

    if (true) { // +1 if-statement
    } else {
    }

    for (int i = 0; i < 10; i++) { // +1 for-statement
    }

    label:// +0
    while (false) { // +1 while-statement
      continue; // +1 continue-statement
    }

    do { // +1 do-while-statement
    } while (false);

    try { // +1 try-statement
      throw new RuntimeException(); // +1 throw-statement
    } catch (Exception e) {
    } finally {
    }

    switch (ch) { // +1 switch-statement
      case 'a':
      case 'b':
      default:
        break; // +1 break-statement
    }

    synchronized (this) { // +1 synchronized-statement
    }

    ; // +1 empty-statement

    return; // +1 return-statement
  }
}
