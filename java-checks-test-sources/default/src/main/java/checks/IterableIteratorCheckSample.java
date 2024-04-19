package checks;

import java.util.Iterator;

class IterableIteratorCheckSample implements Iterator<S4348_Foo>, Iterable<S4348_Foo> {
    private S4348_Foo[] seq;
    private int idx = 0;
    public boolean hasNext() {
        return idx < seq.length;
    }

    void plop() {
        return;
    }

    public S4348_Foo next() {
        return seq[idx++];
    }
    public Iterator<S4348_Foo> iterator() {
        return this; // Noncompliant {{Refactor this code so that the Iterator supports multiple traversal}}
    }
    public Iterator<S4348_Foo> someMethod() {
        return this; // compliant
    }
}
class ValidFooIterator implements Iterator<S4348_Foo>, Iterable<S4348_Foo> {
    private S4348_Foo[] seq;
    private int idx = 0;
    public boolean hasNext() {
        return idx < seq.length;
    }

    public S4348_Foo next() {
        return seq[idx++];
    }

    public Iterator<S4348_Foo> iterator() {
      Object o = new Object() {};
      return null;
    }
}
class S4348_Foo {

    S4348_Foo plop() {
        return this;
    }
}

class S4348_FooIterator2 implements Iterator<S4348_Foo> {
    private S4348_Foo[] seq;
    private int idx = 0;
    public boolean hasNext() {
        return idx < seq.length;
    }

    public S4348_Foo next() {
        return seq[idx++];
    }

    public Iterator<S4348_Foo> iterator() {
        return this;
    }
}
class S4348_FooIterator3 implements Iterable<S4348_Foo> {
    private S4348_Foo[] seq;
    private int idx = 0;
    public boolean hasNext() {
        return idx < seq.length;
    }

    public S4348_Foo next() {
        return seq[idx++];
    }

    public Iterator<S4348_Foo> iterator() {
        return this.iterator();
    }
}
