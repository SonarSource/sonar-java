public class Dit extends One { // Noncompliant {{This class has 3 parents which is greater than 2 authorized.}}

  void foo() {
    Object o = new Dit() { // Noncompliant {{This class has 4 parents which is greater than 2 authorized.}}

    };
  }
}

class One extends Two {
}

class Two {

}
