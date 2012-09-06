public class HelloWorld {

  public void sayHello() {
    switch (ch) {
      case 'a':
        break; // OK
    }

    for (int i = 0; i < 10; i++) {
      break; // NOK
    }
  }

}
