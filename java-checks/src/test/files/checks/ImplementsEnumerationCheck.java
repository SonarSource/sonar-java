class A implements Foo,
                   Enumeration, // Noncompliant {{Implement Iterator rather than Enumeration.}}
                   Iterable {
}

class B implements Foo {        // Compliant
}

class C {                       // Compliant
}

enum D implements Enumeration<Integer> { // Noncompliant
}

class E implements java.util.Enumeration { // Compliant - limitation
}

interface Foo extends Enumeration { // Noncompliant
}
