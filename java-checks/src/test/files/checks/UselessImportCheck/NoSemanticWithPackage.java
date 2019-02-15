@ParametersAreNonnullByDefault
package checks.UselessImportCheck;
import a.b.c.Foo;
import a.b.c.Bar;
import a.b.c.Baz;
import a.b.c.Qux;
import a.b.c.ReferencedFromJavadoc;
import static a.b.c.Foo.d;
import a.b.c.*;
import static a.b.c.Foo.*;
import a.b.c.MyException;
import a.b.c.MyException2;
import a.b.c.MyAnnotation1;
import a.b.c.MyAnnotation2;
import a.b.c.MyAnnotation3;
;
import checks.UselessImportCheckClose.*;
import static checks.UselessImportCheck.Foo.*;
import checks.UselessImportCheck.foo.*;
import checks.UselessImportCheck.foo.Foo;
import pkg.CompliantClass1;
import pkg.CompliantClass2;
import pkg.CompliantClass3;
import pkg.CompliantClass4;
import java.lang.reflect.Array;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.lang.annotation.*;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import javax.annotation.Nonnull;

import static checks.UselessImportCheck.Foo2.A.FLUP; // compliant, used outside of owning class
import static checks.UselessImportCheck.Foo2.A.qix; // compliant : Method symbols are ignored.

public class Foo2 extends Foo {
  Bar a = new Baz<String>();
  Map<@Nonnull String, @Nonnull String> modulesMap = new HashMap<>();
  @Qux
  void test() throws MyException, MyException2 {
  }
  // ReferencedFromJavadoc
  @a.b.c.NonCompliant
  a.b.c.NonCompliant foo(a.b.c.NonCompliant bar) {
    List<CompliantClass1> ok = ImmutableList.<CompliantClass4>of();
    Class ok2 = CompliantClass2.class;
    CompliantClass3.staticMethod("OK");
    pkg.NonCompliant1 ok3;
    Array ok4;
    tottttt a;
    System.out.println(something.t);
    foo(ArrayList::new);
    return new a.b.c.NonCompliant();
  }
  void foo(@Nullable int x){
    System.out.println(FLUP);;
  }
  static class A {
    public static final String BAR = "value";
    public static final String FLUP = "value";
    public static void qix() {}
    byte @MyAnnotation2 [] table = null;
    org.foo.@MyAnnotation1 B myB;


    void foo(java.util.List<String> list) {
      for (@MyAnnotation3 Object o : list) {
        o.toString();
      }
    }

    void foo() {
      System.out.println(BAR);
      qix();
    }
  }

}
