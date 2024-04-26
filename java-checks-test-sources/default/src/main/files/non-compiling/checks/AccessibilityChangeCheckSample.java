package checks;

import java.lang.reflect.Field;

class AccessibilityChangeCheckSample {
  record Person(String name, int age) {
  }

  void fieldModificationIsAlwaysCompliantForRecords() throws NoSuchFieldException, IllegalAccessException, ClassNotFoundException {
    Person person = new Person("A", 26);

    Field uninitializedField;
    uninitializedField.setAccessible(true); // Noncompliant
    uninitializedField.set(person, "B"); // Noncompliant

    Field fieldOfUnknownOrigin = unknown();
    fieldOfUnknownOrigin.setAccessible(true); // Noncompliant
    fieldOfUnknownOrigin.set(person, "B"); // Noncompliant

    Field fieldFromConstructor = new Field(){};
    fieldFromConstructor.setAccessible(true); // Noncompliant
    fieldFromConstructor.set(person, "B"); // Noncompliant

    Field fieldFromUnkwownClass = Unknown.class.getField("myField");
    fieldFromUnkwownClass.setAccessible(true); // Noncompliant
    fieldFromUnkwownClass.set(person, "B"); // Noncompliant

    Field fieldFromUnkwownClass = getUnknownClass().getField("myField");
    fieldFromUnkwownClass.setAccessible(true); // Noncompliant
    fieldFromUnkwownClass.set(person, "B"); // Noncompliant
  }

  Field getAField() {
    return Person.class.getDeclaredFields()[0];
  }
}
