import a.b.c.Foo;                   // Compliant
import a.b.c.Bar;                   // Compliant
import a.b.c.Baz;                   // Compliant
import a.b.c.Qux;                   // Compliant
import a.b.c.ReferencedFromJavadoc; // Compliant
import a.b.c.NonCompliant;          // Non-Compliant
import NonCompliant2;               // Non-Compliant
import static a.b.c.Foo.d;          // Compliant
import a.b.c.*;                     // Compliant
import static a.b.c.Foo.*;          // Compliant

class Foo extends Foo {
  Bar a = new Baz<String>();

  @Qux
  void test() {
  }

  // ReferencedFromJavadoc

  @a.b.c.NonCompliant
  a.b.c.NonCompliant foo(a.b.c.NonCompliant bar) {
    return new a.b.c.NonCompliant();
  }
}