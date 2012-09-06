public class HelloWorld {

  public HelloWorld() {
    super(); // +1
  }

  public void sayHello() {
    int localVar; // +1
    localVar = 42; // +1

    assert true : "not true"; // +1

    if (true) { // +1
    } else { // +1
    }

    for (int i = 0; i < 10; i++) { // +1
    }

    label: // +1
    while (false) { // +1
      continue; // +1
    }

    do { // +1
    } while (false);

    try {
      throw new RuntimeException(); // +1
    } catch (Exception e) { // +1
    }

    switch (ch) { // +1
      case 'a': // +1
      case 'b': // +1
      default: // +1
        break; // +1
    }

    synchronized (this) { // +1
    }

    return; // +1
  }

}
