package checks.UselessImportCheck;

import java.util.Map;
// fix@qf1 {{Remove the import}}
// edit@qf1 [[sl=6;sc=1;el=7;ec=1]] ̣{{}}
import a.b.c.NotReferencedFromJavadoc; // Noncompliant [[sc=8;ec=38;quickfixes=qf1]]
import java.util.List;
import javax.annotation.Nonnull;
//fix@qf2 {{Remove the import}}
//edit@qf2 [[sl=11;sc=1;el=12;ec=1]] ̣{{}}
import java.lang.String;            // Noncompliant  {{Remove this unnecessary import: java.lang classes are always implicitly imported.}} [[sc=8;ec=24;quickfixes=qf2]]
import java.lang.*;                 // Noncompliant  {{Remove this unnecessary import: java.lang classes are always implicitly imported.}} [[sc=8;ec=19;quickfixes=qf3]]
//fix@qf3 {{Remove the import}}
//edit@qf3 [[sl=12;sc=1;el=15;ec=1]] ̣{{}}
import javax.annotation.Nullable;
import a.b.c.Foo;
import a.b.c.Foo;                   // Noncompliant  {{Remove this duplicated import.}} [[sc=8;ec=17;quickfixes=qf4]]
//fix@qf4 {{Remove the import}}
//edit@qf4 [[sl=17;sc=1;el=21;ec=1]] ̣{{}}

import checks.UselessImportCheck.*; // Noncompliant  {{Remove this unnecessary import: same package classes are always implicitly imported.}} [[sc=8;ec=35;quickfixes=qf5]]
//fix@qf5 {{Remove the import}}
//edit@qf5 [[sl=21;sc=1;el=25;ec=1]] ̣{{}}

import java.util.ArrayList;
import java.util.HashMap;

import static checks.UselessImportCheck.SomeEntity.FIND_BY_AGE;    // Noncompliant [[sc=15;ec=63;quickfixes=qf6]]
//fix@qf6 {{Remove the static import}}
//edit@qf6 [[sl=28;sc=1;el=31;ec=1]] ̣{{}}
import static checks.UselessImportCheck.Foo2WithQuickFixes.A.FLUP; // compliant, used outside of owning class
import static checks.UselessImportCheck.Foo2WithQuickFixes.A.qix;  // Noncompliant [[sc=15;ec=65;quickfixes=qf7]]
//fix@qf7 {{Remove the static import}}
//edit@qf7 [[sl=31;sc=67;el=32;ec=66]] ̣{{}}

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
