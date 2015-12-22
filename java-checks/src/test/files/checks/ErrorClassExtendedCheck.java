class A extends Error { // Noncompliant {{Extend "java.lang.Exception" or one of its subclasses.}} [[sc=17;ec=22]]
}

class B extends java.lang.Error { // Noncompliant [[sc=17;ec=32]]
}

class C { // Compliant
}

class D extends Exception { // Compliant
}

class E extends Error.foo { // Compliant
}

class F extends java.lang.Exception { // Compliant
}

class G extends java.foo.Error { // Compliant
}

class H extends foo.lang.Error { // Compliant
}
