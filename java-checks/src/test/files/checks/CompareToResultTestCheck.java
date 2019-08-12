class MyComparable implements Comparable<MyComparable> {
  
  int compareToField = compareTo(new MyComparable());
  
  public int compareTo(MyComparable other) {
    return 0;
  }

  int field;

  public void aMethod(MyComparable other, NotComparable notComparable) {
    if (compareTo(other) == -1) {} // Noncompliant [[sc=26;ec=28]] {{Only the sign of the result should be examined.}}
    if (compareTo(other) == -5) {} // Noncompliant
    if (compareTo(other) == 0) {}
    if (compareTo(other) == 1) {} // Noncompliant
    if (compareTo(other) != 1) {} // Noncompliant
    if (other.compareTo(this) == 1) {} // Noncompliant
    if (-1 == compareTo(other)) {} // Noncompliant 
    boolean result = compareTo(other) == -1; // Noncompliant
    if (notComparable.compareTo(other) == 1) {}
    if (compareTo(other) == hashCode()) {}
    if (compareTo(other) == - hashCode()) {}
    if (compareTo(other, other) == 1) {}
    if (1 == compareTo("", "", "")) {}
    //false positive:
    if (1 == compareTo(notComparable)) {} // Noncompliant
    if (0 == compareTo(other)) {}
    
    int c1 = compareTo(other);
    if (c1 == 1) {} // Noncompliant
    
    int c2 = compareTo(other);
    c2 = compareTo(other, other);
    if (c2 == 1) {}

    int c3 = compareTo(other);
    c3++;
    if (c3 == 1) {}
    
    int c4;
    if (c4 == 1) {}
    
    int c5 = 1;
    if (c5 == 1) {}
    
    int c6 = compareTo(other, other);
    if (c6 == 1) {}
    
    if (compareTo(other) + 1 == 1) {}
    if (compareToField == 1) {}    
    if (unknownVar == 1) {}

    this.field = compareTo(other);
    this.field++;
    if (this.field == 1) {}
  }
  
  public int compareTo(NotComparable o2) {
    return 0;
  }

  public int compareTo(MyComparable other1, MyComparable other2) {
    return 0;
  }

}

class NotComparable {

  public int compareTo(MyComparable other) {
    return 0;
  }

  public void aMethod(MyComparable other) {
    if (compareTo(other) == -1) {}
  }
  
}
