class A {

  void foo() {
    javax.servlet.http.HttpSession session = request.getSession();
    session.setAttribute("address", new Address()); // Noncompliant [[sc=37;ec=50]] {{Make "Address" serializable or don't store it in the session.}}
    session.setAttribute("person", new Person()); // Noncompliant {{Make "Person" serializable or don't store it in the session.}}
    session.setAttribute("person", 1);
    session.setAttribute("person", new Integer(1));
    session.setAttribute("addressString", "address");
  }

  public class Address {
  }
  public class Person {
  }
}
