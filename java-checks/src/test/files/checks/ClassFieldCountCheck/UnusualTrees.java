enum TooManyFieldsEnum { // Noncompliant {{Refactor this class so it has no more than 2 fields, rather than the 3 it currently has.}}
//   ^^^^^^^^^^^^^^^^^
  ENUM_CONSTANT_TREE { // Noncompliant {{Refactor this class so it has no more than 2 fields, rather than the 3 it currently has.}}
//^^^^^^^^^^^^^^^^^^
     int field1;
     int field2;
     int field3;
  };
   int field1;
   int field2;
   int field3;
}

interface TooManyFieldsInterface { // Compliant - interface fields are effectively static and final
   int field1;
   int field2;
   int field3;
}

class TooManyFieldsAnonymous {
  {
    new java.util.ArrayList<Integer>() { // Noncompliant {{Refactor this class so it has no more than 2 fields, rather than the 3 it currently has.}}
//      ^^^^^^^^^^^^^^^^^^^^^^^^^^^^
       int field1;
       int field2;
       int field3;
    };
  }
}
