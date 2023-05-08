import java.io.*;
import java.util.*;

class A {
  void plop() {
    FileInputStream stream = new FileInputStream("myFile"); // Noncompliant  this is an FP, issue will be raised because we hit max steps before reaching the close
    stream.read();

    boolean a = true;
    a &= (Math.random() == 1.0d);
    a &= (Math.random() == 2.0d);
    a &= (Math.random() == 3.0d);
    a &= (Math.random() == 4.0d);
    a &= (Math.random() == 5.0d);
    a &= (Math.random() == 6.0d);
    a &= (Math.random() == 7.0d);
    a &= (Math.random() == 8.0d);
    a &= (Math.random() == 9.0d);
    a &= (Math.random() == 10.0d);
    a &= (Math.random() == 11.0d);
    a &= (Math.random() == 12.0d);
    a &= (Math.random() == 13.0d);
    a &= (Math.random() == 14.0d);

    if (a) { //BOOM : 2^n -1 states are generated (where n is the number of lines of &= assignements in the above code) -> fail fast by not even enqueuing nodes
      stream.close();
    }

  }
}
