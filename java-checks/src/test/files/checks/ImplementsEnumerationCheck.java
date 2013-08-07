class A implements Foo,
                   Enumeration, // Non-Compliant
                   Iterable {
}

class B implements Foo {        // Compliant
}

class C {                       // Compliant
}

enum D implements Enumeration<Integer> { // Non-Compliant
}

class E implements java.util.Enumeration { // Compliant - limitation
}

interface Foo extends Enumeration { // Non-Compliant
}
