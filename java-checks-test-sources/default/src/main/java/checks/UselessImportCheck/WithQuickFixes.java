package checks.UselessImportCheck;

import java.util.Map;
// fix@qf1 {{Remove the import}}
// edit@qf1 [[sl=6;sc=1;el=8;ec=1]] ̣{{}}
import a.b.c.NotReferencedFromJavadoc; // Noncompliant [[quickfixes=qf1]]
//     ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
import java.util.List;
import javax.annotation.Nonnull;
//fix@qf2 {{Remove the import}}
//edit@qf2 [[sl=12;sc=1;el=14;ec=1]] ̣{{}}
import java.lang.String;            // Noncompliant [[quickfixes=qf2]]
//     ^^^^^^^^^^^^^^^^
import java.lang.*;                 // Noncompliant [[quickfixes=qf3]]
//     ^^^^^^^^^^^
//fix@qf3 {{Remove the import}}
//edit@qf3 [[sl=14;sc=1;el=18;ec=1]] ̣{{}}
import javax.annotation.Nullable;
import a.b.c.Foo;
import a.b.c.Foo;                   // Noncompliant [[quickfixes=qf4]]
//     ^^^^^^^^^
//fix@qf4 {{Remove the import}}
//edit@qf4 [[sl=20;sc=1;el=25;ec=1]] ̣{{}}

import checks.UselessImportCheck.*; // Noncompliant [[quickfixes=qf5]]
//     ^^^^^^^^^^^^^^^^^^^^^^^^^^^
//fix@qf5 {{Remove the import}}
//edit@qf5 [[sl=25;sc=1;el=30;ec=1]] ̣{{}}

import java.util.ArrayList;
import java.util.HashMap;

import static checks.UselessImportCheck.SomeEntity.FIND_BY_AGE;    // Noncompliant [[quickfixes=qf6]]
//            ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
//fix@qf6 {{Remove the static import}}
//edit@qf6 [[sl=33;sc=1;el=37;ec=1]] ̣{{}}
import static checks.UselessImportCheck.Foo2WithQuickFixes.A.FLUP; // compliant, used outside of owning class
import static checks.UselessImportCheck.Foo2WithQuickFixes.A.qix;  // Noncompliant [[quickfixes=qf7]]
//            ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
//fix@qf7 {{Remove the static import}}
//edit@qf7 [[sl=37;sc=67;el=38;ec=66]] ̣{{}}

class Foo2WithQuickFixes extends Foo {

  @Nonnull
  Map<String,String> map = new HashMap<>();
  List<String> list = new ArrayList<>();

  void foo(@Nullable int x){
    System.out.println(FLUP);
  }

  static class A {
    public static final String BAR = "value";
    public static final String FLUP = "value";
    public static void qix() {}
  }
}
