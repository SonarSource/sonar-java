class A {

  void foo() {
    javax.servlet.http.HttpSession session = request.getSession();
    session.setAttribute("address", new Address()); // Noncompliant [[sc=37;ec=50]] {{Make "Address" serializable or don't store it in the session.}}
    session.setAttribute("person", new Person()); // Noncompliant {{Make "Person" serializable or don't store it in the session.}}
    session.setAttribute("person", 1);
    session.setAttribute("person", new Integer(1));
    session.setAttribute("addressString", "address");

    session.setAttribute("intArray", new int[] { 1, 2 });
    session.setAttribute("stringArray", new String[] { "one", "two" });
    session.setAttribute("personArray", new Person[] { new Person() }); // Noncompliant {{Make "Person[]" serializable or don't store it in the session.}}

    session.setAttribute("stringList", new java.util.ArrayList<>(java.util.Arrays.asList("one", "two")));
    session.setAttribute("personList", new java.util.ArrayList<>(java.util.Arrays.asList(new Person(), new Person()))); // Noncompliant

    session.setAttribute("nonSerializableParameterized", new CustomStack<String>()); // Noncompliant {{Make "CustomStack" serializable or don't store it in the session.}}
  }

  public class Address {
  }
  public class Person {
  }
  public class CustomStack<E> {
  }
}
