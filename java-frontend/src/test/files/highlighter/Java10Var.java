import java.util.ArrayList;
import java.net.URL;
import java.io.BufferedReader;
import java.io.InputStreamReader;

class A {

  void foo() throws Exception {
    // various usages of 'var'
    var a = 2;

    var list = new ArrayList<String>();

    list.add("hello");
    list.add("world");

    for (var counter = 0; counter < list.size(); counter++) {
      var value = list.get(counter);
    }

    for (var value : list) {
      value.length();
    }

    var url = new URL("http://www.oracle.com/");
    var conn = url.openConnection();
    try (var reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
      var stream = reader.lines();
    }

    // anonymous class with new members are visible from context of declaration
    var myA = new A() {

      Object myField;

      @Override
      public void foo() {
        this.myField = bar();
      }

      String bar() {
        return "42";
      }
    };

    myA.bar();
    if (myA.myField != null) {
      myA.foo();
    }

    Object var;
  }
}
