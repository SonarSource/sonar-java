@ParametersAreNonnullByDefault
package checks.UselessImportCheck;
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
import a.b.c.MyException;           // Compliant
import a.b.c.MyException2;          // Compliant
import java.lang.String;            // Non-Compliant
import java.lang.*;                 // Non-Compliant
import a.b.c.Foo;                   // Non-Compliant

;  // Compliant

import checks.UselessImportCheck.*;              // Non-Compliant
import checks.UselessImportCheckClose.*;         // Compliant
import static checks.UselessImportCheck.Foo.*;   // Compliant
import checks.UselessImportCheck.foo.*;          // Compliant

import pkg.NonCompliant1;           // Non-Compliant

import pkg.CompliantClass1;         // Compliant
import pkg.CompliantClass2;         // Compliant
import pkg.CompliantClass3;         // Compliant
import pkg.CompliantClass4;         // Compliant

import java.lang.reflect.Array;     // Compliant

import javax.annotation.Nullable;   //Compliant
import javax.annotation.ParametersAreNonnullByDefault; // Compliant

import java.lang.annotation.*;      // Compliant
import java.util.ArrayList;         // Compliant
import java.util.Map;               // Compliant
import java.util.HashMap;           // Compliant
import javax.annotation.Nonnull;    // Compliant

class Foo2 extends Foo {
  Bar a = new Baz<String>();
  
  Map<@Nonnull String, @Nonnull String> modulesMap = new HashMap<>();

  @Qux
  void test() throws MyException, MyException2 {
  }

  // ReferencedFromJavadoc

  @a.b.c.NonCompliant
  a.b.c.NonCompliant foo(a.b.c.NonCompliant bar) {
    List<CompliantClass1> ok = ImmutableList.<CompliantClass4>of();
    Class ok = CompliantClass2.class;
    CompliantClass3.staticMethod("OK");

    pkg.NonCompliant1 ok;

    Array ok;

    tottttt a;

    System.out.println(something.t);
    foo(ArrayList::new);
    return new a.b.c.NonCompliant();
  }
  void foo(@Nullable int x){}

}

