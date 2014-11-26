class MyComparable implements Comparable<MyComparable> {
  
  public int compareTo(MyComparable other) {
    return 0;
  }
  
  public void aMethod(MyComparable other, NotComparable notComparable) {
    if (compareTo(other) == -1) {} // Noncompliant
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
    if (compareTo("", "", "") == 1) {}
    if (compareTo(notComparable) == 1) {} // False positive...
    if (0 == compareTo(other)) {}
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