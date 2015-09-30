class A {
  void fun(){
    Integer j;
    B b;
    B[] arrayB;



    if(j instanceof Integer) {} // Noncompliant {{Remove this useless "instanceof" operator; it will always return "true". }}
    if(b instanceof A) {} // always false, won't compile, no issue raised.
    if(b instanceof I) {} // Noncompliant
    if(b instanceof Object) {} // Noncompliant
    if(arrayB instanceof B[]){} // Noncompliant
    if(arrayB instanceof I[]){} // Noncompliant

  }
}

interface I { }
class B implements I { }
