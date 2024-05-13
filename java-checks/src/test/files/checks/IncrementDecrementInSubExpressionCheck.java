class Foo {

  public void f() {
    i++;
    ++i;
    i--;
    --i;

    foo[i]++;

    foo[i++] = 0; // Noncompliant {{Extract this increment or decrement operator into a dedicated statement.}}
//      ^^^
    foo[i--] = 0; // Noncompliant
    foo[++i] = 0; // Noncompliant
    foo[--i] = 0; // Noncompliant

    foo[~i] = 0;
    register(() -> i++);

    return i++;
    return ++i;
    return foo[++i]; // Noncompliant
    return;
  }

  void register(Runnable runnable) {}
}
