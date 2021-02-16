import java.io.*;
import java.util.*;

class A {
  void plop() {
    FileInputStream stream = new FileInputStream("myFile"); // Noncompliant  this is an FP, issue will be raised because we hit max steps before reaching the close
    stream.read();

    boolean a = true;
    a &= (b() == C);
    a &= (b() == C);
    a &= (b() == C);
    a &= (b() == C);
    a &= (b() == C);
    a &= (b() == C);
    a &= (b() == C);
    a &= (b() == C);
    a &= (b() == C);
    a &= (b() == C);
    a &= (b() == C);
    a &= (b() == C);
    a &= (b() == C);
    a &= (b() == C);

    if (a) { //BOOM : 2^n -1 states are generated (where n is the number of lines of &= assignements in the above code) -> fail fast by not even enqueuing nodes
      stream.close();
    }

  }
}
