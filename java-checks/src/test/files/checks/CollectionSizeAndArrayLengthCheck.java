import java.util.Collection;
import java.util.List;

abstract class A implements List<String> {
  String value = "42";
  int length = -1;
  int field = 42;
  int[] ray = {};

  void foo(Collection<String> coll, String[] arr, B b) {

    if (size() >= 0) { }           // Noncompliant {{The size of a collection is always ">=0", so update this test to use isEmpty().}}
    if (0 >= size()) { }           // Compliant - not always true or false

    if (getList().size() < 0) { }  // Noncompliant {{The size of a collection is always ">=0", so update this test to use isEmpty().}}
    if (coll.size() >= 0) { }      // Noncompliant {{The size of "coll" is always ">=0", so update this test to use isEmpty().}}
    if (0 <= coll.size()) { }      // Noncompliant {{The size of "coll" is always ">=0", so update this test to use isEmpty().}}

    if (arr.length >= 0) { }       // Noncompliant {{The length of "arr" is always ">=0", so update this test to either "==0" or ">0".}}
    if (getArray().length < 0) { } // Noncompliant {{The length of an array is always ">=0", so update this test to either "==0" or ">0".}}
    if (this.ray.length >= 0) { }  // Noncompliant {{The length of "ray" is always ">=0", so update this test to either "==0" or ">0".}}
    if (0 <= arr.length) { }       // Noncompliant {{The length of "arr" is always ">=0", so update this test to either "==0" or ">0".}}

    if (0 > coll.size()) { }       // Noncompliant {{The size of "coll" is always ">=0", so update this test to use isEmpty().}}
    if (0 > arr.length)  { }       // Noncompliant {{The length of "arr" is always ">=0", so update this test to either "==0" or ">0".}}
    if (arr.length > 0)  { }       // Compliant

    if (coll.size() == 0) { }      // Compliant - will be handled by S1155
    if (b.size() >= 0) { }         // Compliant - not a collection

    if (arr.length >= 42) { }      // Compliant
    if (this.length >= 0) { }      // Compliant - not an array
    if (value.length() >= 0) { }   // Compliant - not an array

    if (this.field >= 0) { }
    if (field >= 0) { }
    if (0 <= 0) { }
  }

  abstract List<String> getList();
  abstract String[] getArray();

  interface B { public int size(); }
}
