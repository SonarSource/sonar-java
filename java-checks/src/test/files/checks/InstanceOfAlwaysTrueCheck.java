class A {
  void fun(){
    Integer j;
    B b;
    B[] arrayB;



    if(j instanceof Integer) {} //Noncompliant
    if(b instanceof A) {} // always false, won't compile, no issue raised.
    if(b instanceof I) {}//Noncompliant
    if(b instanceof Object) {}//Noncompliant
    if(arrayB instanceof B[]){}//Noncompliant
    if(arrayB instanceof I[]){}//Noncompliant

  }
}

interface I { }
class B implements I { }