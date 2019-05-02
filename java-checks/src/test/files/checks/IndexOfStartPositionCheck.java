
class A {
  String ae() {
    return "ae";
  }

  void nonCompliant() {
    String name = "ismael";

    if (name.indexOf(ae()) > 2) { // Noncompliant [[sc=14;ec=21]] {{Use ".indexOf(xxx,n) > -1" instead.}}
      // ...
    }
    if (name.indexOf("ae") > 1) { // Noncompliant {{Use ".indexOf("ae",n) > -1" instead.}}
      // ...
    }
    String ae = "ae";
    if (2 < (name.indexOf(ae))) { // Noncompliant {{Use ".indexOf(ae,n) > -1" instead.}}
      // ...
    }
  }

  int value() {}

  void compliant() {
    String name = "ismael";

    if (name.indexOf("ae", 2) > -1) {
      // ...
    }
    if (-1 < name.indexOf("ae", 2)) {
      // ...
    }
    if (name.indexOf("ae") > -1) {

    }
    if (name.indexOf("is") == 0) {

    }
    if (name.indexOf("ae") > value()) {

    }

    // coverage
   boolean b = 1 > 2;
  }
}
