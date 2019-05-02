class A {

  static class Inner {
    public String s1;
  }

  void method() {
    String s = "";
    Inner inner = new Inner();
    int i = 0;
    for (; i < 10; i++) {
      s = i + " : " + s; // Noncompliant {{Use a StringBuilder instead.}}
      s += i;// Noncompliant {{Use a StringBuilder instead.}}
      inner.s1 = i + " : " + inner.s1; // Noncompliant {{Use a StringBuilder instead.}}
    }
    for (Object i : list) {
      s = i + " : " + s; // Noncompliant {{Use a StringBuilder instead.}}
    }
    do {
      s = i + " : " + s; // Noncompliant {{Use a StringBuilder instead.}}
    } while (i < 20);
    while (i < 20) {
      s = i + " : " + s; // Noncompliant {{Use a StringBuilder instead.}}
      inner.s1 = i + inner.s1 + " : "; // Noncompliant {{Use a StringBuilder instead.}}
      s = ((s + " : ")); // Noncompliant {{Use a StringBuilder instead.}}
      inner = new Inner();
      i = i + 1;
      s = i + ":"; //Compliant
      inner.s1 = i + ":";
    }
    s += "a" + "b";
    s = s + "a" + "b";
    for (int i = 0; i < 10; i++) {
      MyObject myObject = new MyObject();
      myObject.stringProperty = "a" + "b";  //Compliant, var is local in the loop
      myObject.stringProperty += "a";//Compliant, var is local in the loop
      MyObject.newInstance().stringProperty = "a" + "b"; //Compliant
      MyObject.newInstance().stringProperty += "b"; // Noncompliant {{Use a StringBuilder instead.}}
      MyObject.newInstance().stringProperty = "b" + MyObject.newInstance().stringProperty; // Noncompliant {{Use a StringBuilder instead.}}
    }
    for (int i=0; i < array.length; i++) {
      array[i] = "a" + array[i]; // Compliant, it is an array access
    }
  }

  class MyObject {
    String stringProperty;

    static MyObject newInstance() {
      return new MyObject();
    }
  }

}
