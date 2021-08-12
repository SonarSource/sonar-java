class A {  // Noncompliant [[sc=1;ec=6;quickfixes=qf1,qf2]]
  // fix@qf1 {{Description}}
  // edit@qf1 [[sc=7;ec=8]] {{Replacement}}

  // fix@qf2 {{Description2}}
  // edit@qf2 [[sc=2;ec=3]] {{Replacement2}}
}
