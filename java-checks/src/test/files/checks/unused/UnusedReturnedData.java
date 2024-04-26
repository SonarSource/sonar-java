import java.util.Iterator;

public class Class {

  public boolean method() {
    java.util.Iterator<Boolean> itr;
    final java.io.Reader rd;
    java.io.BufferedReader brd;

    rd.read(); // Noncompliant {{Use or store the value returned from "read" instead of throwing it away.}}
//     ^^^^
    brd.readLine(); // Noncompliant {{Use or store the value returned from "readLine" instead of throwing it away.}}
//      ^^^^^^^^

    String line = brd.readLine(); // Compliant
    line = brd.readLine(); // Compliant
    int value = rd.read(); // Compliant
    value = rd.read(); // Compliant

    // call from a derived class
    brd.read(); // Noncompliant {{Use or store the value returned from "read" instead of throwing it away.}}

    // call nested in assignment
    InnerClass cls = new InnerClass() {
      public void innerMethod() {
        rd.read(); // Noncompliant {{Use or store the value returned from "read" instead of throwing it away.}}
      }
    };
    cls = new InnerClass() {
      public void innerMethod() {
        rd.read(); // Noncompliant {{Use or store the value returned from "read" instead of throwing it away.}}
      }
    };

    // statements
    do {
      rd.read(); // Noncompliant {{Use or store the value returned from "read" instead of throwing it away.}}
    } while (brd.readLine() == null); // Noncompliant {{Use or store the value returned from "readLine" instead of throwing it away.}}
    do {
    } while ((brd.readLine() == null) && true); // Noncompliant {{Use or store the value returned from "readLine" instead of throwing it away.}}
    do {
    } while (brd.readLine() == "string"); // Compliant
    do {
    } while (brd.readLine() + null); // Compliant
    do {
    } while (null != brd.readLine()); // Noncompliant {{Use or store the value returned from "readLine" instead of throwing it away.}}
    do {
    } while ("string" != brd.readLine()); // Compliant
    do {
    } while (rd.read() != 0); // Compliant

    for (brd.readLine();;) { // Noncompliant {{Use or store the value returned from "readLine" instead of throwing it away.}}
    }
    for (rd.read();;) { // Noncompliant {{Use or store the value returned from "read" instead of throwing it away.}}
    }

    for (; brd.readLine() == null;) { // Noncompliant {{Use or store the value returned from "readLine" instead of throwing it away.}}
    }
    for (; brd.readLine() != null;) { // Noncompliant {{Use or store the value returned from "readLine" instead of throwing it away.}}
    }
    for (; rd.read() != 0;) { // Compliant
    }

    for (;; brd.readLine()) { // Noncompliant {{Use or store the value returned from "readLine" instead of throwing it away.}}
    }
    for (;; rd.read()) { // Noncompliant {{Use or store the value returned from "read" instead of throwing it away.}}
    }

    for (;;) {
      rd.read(); // Noncompliant {{Use or store the value returned from "read" instead of throwing it away.}}
    }

    if ((brd.readLine()) == null) { // Noncompliant {{Use or store the value returned from "readLine" instead of throwing it away.}}
      rd.read(); // Noncompliant {{Use or store the value returned from "read" instead of throwing it away.}}
    } else {
      rd.read(); // Noncompliant {{Use or store the value returned from "read" instead of throwing it away.}}
    }
    if (brd.readLine() != (null)) { // Noncompliant {{Use or store the value returned from "readLine" instead of throwing it away.}}
    }
    if (rd.read() == 0) { // Compliant
    }

    switch (brd.readLine()) { // Compliant
      case "string":
        rd.read(); // Noncompliant {{Use or store the value returned from "read" instead of throwing it away.}}
    }
    switch (brd.readLine() != null) { // Noncompliant {{Use or store the value returned from "readLine" instead of throwing it away.}}
      case TRUE:
        rd.read(); // Noncompliant {{Use or store the value returned from "read" instead of throwing it away.}}
    }
    switch (rd.read()) { // Compliant
      case 0:
        rd.read(); // Noncompliant {{Use or store the value returned from "read" instead of throwing it away.}}
    }

    while (brd.readLine() != null) { // Noncompliant {{Use or store the value returned from "readLine" instead of throwing it away.}}
    }
    while (rd.read() == 0) { // Compliant
    }

    while ((str = brd.readLine()) != null) { // Compliant
    }

    return true;
  }

}
