import java.util.Iterator;
class FooIterator implements Iterator<Foo>, Iterable<Foo> {
    private Foo[] seq;
    private int idx = 0;
    public boolean hasNext() {
        return idx < seq.length;
    }

    void plop() {
        return;
    }

    public Foo next() {
        return seq[idx++];
    }
    public Iterator<Foo> iterator() {
        return this; // Noncompliant {{Refactor this code so that the Iterator supports multiple traversal}}
    }
    public Iterator<Foo> someMethod() {
        return this; // compliant
    }
}
class ValidFooIterator implements Iterator<Foo>, Iterable<Foo> {
    private Foo[] seq;
    private int idx = 0;
    public boolean hasNext() {
        return idx < seq.length;
    }

    public Foo next() {
        return seq[idx++];
    }

    public Iterator<Foo> iterator() {
        return somethingElse;
    }
}
class Foo{

    Foo plop() {
        return this;
    }
}

class FooIterator2 implements Iterator<Foo> {
    private Foo[] seq;
    private int idx = 0;
    public boolean hasNext() {
        return idx < seq.length;
    }

    public Foo next() {
        return seq[idx++];
    }

    public Iterator<Foo> iterator() {
        return this;
    }
}
class FooIterator3 implements Iterable<Foo> {
    private Foo[] seq;
    private int idx = 0;
    public boolean hasNext() {
        return idx < seq.length;
    }

    public Foo next() {
        return seq[idx++];
    }

    public Iterator<Foo> iterator() {
        return this;
    }
}
