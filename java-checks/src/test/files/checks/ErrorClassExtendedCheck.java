class A extends Error { // Non-Compliant
}

class B extends java.lang.Error { // Non-Compliant
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
