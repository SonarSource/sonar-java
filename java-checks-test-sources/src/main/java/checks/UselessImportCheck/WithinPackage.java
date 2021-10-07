package checks.UselessImportCheck;

import a.b.c.Foo;
import a.b.c.Bar;
import a.b.c.Baz;
import a.b.c.Qux;
import a.b.c.ReferencedFromJavadoc;
import java.util.Map;
import java.util.Map.Entry;
import a.b.c.NotReferencedFromJavadoc; // Noncompliant
import a.b.c.MyException;
import a.b.c.MyException2;
import a.b.c.NonCompliant; // Noncompliant
import a.b.c.MyAnnotation1;
import a.b.c.MyAnnotation2;
import a.b.c.MyAnnotation3;
import java.lang.String;           // Noncompliant [[sc=8;ec=24]] {{Remove this unnecessary import: java.lang classes are always implicitly imported.}}
import java.lang.*;                 // Noncompliant [[sc=8;ec=19]] {{Remove this unnecessary import: java.lang classes are always implicitly imported.}}
import a.b.c.Foo;                   // Noncompliant [[sc=8;ec=17]] {{Remove this duplicated import.}}

import checks.UselessImportCheck.*;              // Noncompliant {{Remove this unnecessary import: same package classes are always implicitly imported.}}

import com.google.common.collect.ImmutableList;
import pkg.NonCompliant1;           // Noncompliant
import javax.annotation.ParametersAreNonnullByDefault; // Noncompliant
import pkg.CompliantClass1;
import pkg.CompliantClass2;
import pkg.CompliantClass3;
import pkg.CompliantClass4;

import checks.UselessImportCheck.WithPackageAux; // Noncompliant {{Remove this unnecessary import: same package classes are always implicitly imported.}}
import checks.UselessImportCheck.subpackage.WithinSubPackage; // Noncompliant {{Remove this unused import 'checks.UselessImportCheck.subpackage.WithinSubPackage'.}}
import checks.UselessImportCheck.subpackage.*; // Noncompliant {{Remove this unused import 'checks.UselessImportCheck.subpackage'.}}

import java.io.File; // Noncompliant

import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import javax.annotation.Nonnull;

import static checks.UselessImportCheck.SomeEntity.FIND_BY_NAME;
import static checks.UselessImportCheck.SomeEntity.FIND_BY_AGE; // Noncompliant
import static checks.UselessImportCheck.Foo2.A.BAR; // Noncompliant
import static checks.UselessImportCheck.Foo2.A.FLUP; // compliant, used outside of owning class
import static checks.UselessImportCheck.Foo2.A.qix; // Noncompliant


/**
 * @see {@link ReferencedFromJavadoc}
 * @see ReferencedFromJavadoc
 * see NotReferencedFromJavadoc
 */
class Foo2 extends Foo {
  Bar a = new Baz<String>();
  Map<String, String> modulesMap = new HashMap<>();
  @Qux
  void test(@Nonnull String s) throws MyException, MyException2 {
  }
  // ReferencedFromJavadoc
  @a.b.c.NonCompliant
  a.b.c.NonCompliant foo(a.b.c.NonCompliant bar) {
    List<CompliantClass1> ok1 = ImmutableList.<CompliantClass1>of();
    List<CompliantClass4> ok4 = ImmutableList.<CompliantClass4>of();
    Class ok2 = CompliantClass2.class;
    CompliantClass3.staticMethod("OK");
    pkg.NonCompliant1 ok3;
    Array ok5;
    tottttt a;
    CompliantClass1 something = new CompliantClass1();

    System.out.println(something.t);
    new A().foo(new ArrayList<>());
    return new a.b.c.NonCompliant(){
      @Override
      public Class<? extends Annotation> annotationType() {
        return null;
      }
    };
  }
  void foo(@Nullable int x){
    System.out.println(FLUP);;
  }
  static class A {
    public static final String BAR = "value";
    public static final String FLUP = "value";
    public static void qix() {}
    byte @MyAnnotation2 [] table = null;
    pkg.@MyAnnotation1 B myB;


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

  /**
   * FileUtils#getFile() .... <--- should not trigger invalid import of j-a-v-a-.-i-o-.-F-i-l-e (avoid recognition here)
   */
  void bar() {
    // ...
  }

  private class tottttt {
  }
}

@MyAnnotation(name = FIND_BY_NAME)
class SomeEntity {
  public static final String FIND_BY_NAME = "SomeEntity.findByName";
  public static final String FIND_BY_AGE = "SomeEntity.findByAge";
  private String name;

  @MyAnnotation(name = FIND_BY_AGE)
  public String getEntityName() {
    return name;
  }
}

@interface MyAnnotation {
  String name();
}

class MyEntry implements Entry<String, String> {

  @Override
  public String getKey() {
    return null;
  }

  @Override
  public String getValue() {
    return null;
  }

  @Override
  public String setValue(String value) {
    return null;
  }

  @Override
  public boolean equals(Object o) {
    return false;
  }

  @Override
  public int hashCode() {
    return 0;
  }
}
