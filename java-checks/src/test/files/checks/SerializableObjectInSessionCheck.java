class A {

  void foo() {

    javax.servlet.http.HttpSession session = request.getSession();
    session.setAttribute("address", new Address()); //NonCompliant
    session.setAttribute("person", new Person()); //NonCompliant
    session.setAttribute("addressString", "address");
  }

  public class Address {
  }
  public class Person {
  }
}