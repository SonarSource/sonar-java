package my.testpackage;
class A {}
class B extends A {}
class C  extends B {}// Noncompliant {{This class has 3 parents which is greater than 2 authorized.}}
class D  extends C {}
class E  extends D {}
class F  extends E {}
class G  extends F {} // Noncompliant {{This class has 3 parents which is greater than 2 authorized.}}
