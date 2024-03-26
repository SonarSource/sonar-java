package checks;

import java.lang.reflect.Field;

class AccessibilityChangeCheckSample {
  record Person(String name, int age) {
  }

  void fieldModificationIsAlwaysCompliantForRecords() throws NoSuchFieldException, IllegalAccessException, ClassNotFoundException {
    Person person = new Person("A", 26);

    Field uninitializedField;
    uninitializedField.setAccessible(true); // Noncompliant FP Not exploring fields from unkwnon methods
    uninitializedField.set(person, "B"); // Noncompliant FP Not exploring fields from unkwnon methods

    Field fieldOfUnknownOrigin = unknown();
    fieldOfUnknownOrigin.setAccessible(true); // Noncompliant FP Not exploring fields from unkwnon methods
    fieldOfUnknownOrigin.set(person, "B"); // Noncompliant FP Not exploring fields from unkwnon methods

    Field fieldFromConstructor = new Field(){};
    fieldFromConstructor.setAccessible(true); // Noncompliant FP Not exploring fields from unkwnon methods
    fieldFromConstructor.set(person, "B"); // Noncompliant FP Not exploring fields from unkwnon methods

    Field fieldFromUnkwownClass = Unknown.class.getField("myField");
    fieldFromUnkwownClass.setAccessible(true); // Noncompliant FP Not exploring fields from unkwnon methods
    fieldFromUnkwownClass.set(person, "B"); // Noncompliant FP Not exploring fields from unkwnon methods

    Field fieldFromUnkwownClass = getUnknownClass().getField("myField");
    fieldFromUnkwownClass.setAccessible(true); // Noncompliant FP Not exploring fields from unkwnon methods
    fieldFromUnkwownClass.set(person, "B"); // Noncompliant FP Not exploring fields from unkwnon methods
  }

  Field getAField() {
    return Person.class.getDeclaredFields()[0];
  }
}
