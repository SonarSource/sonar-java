package checks;

import java.lang.reflect.Field;

class AccessibilityChangeCheckSample {
  void makeItPublic(String methodName, java.lang.reflect.AccessibleObject[] arr, boolean someBool) throws NoSuchMethodException {
    this.getClass().getMethod(methodName).setAccessible(true); // Noncompliant {{This accessibility update should be removed.}}
    this.getClass().getMethod(methodName).setAccessible(arr, true); // Noncompliant {{This accessibility update should be removed.}}
    this.getClass().getMethod(methodName).setAccessible(arr, false); // compliant
    this.getClass().getMethod(methodName).setAccessible(arr, someBool); // compliant
  }

  void setItAnyway(String fieldName, int value) throws NoSuchFieldException, IllegalAccessException {
    this.getClass().getDeclaredField(fieldName).setInt(this, value); // Noncompliant {{This accessibility bypass should be removed.}}
  }

  record Person(String name, int age) {
  }

  void accessibilityChangeOnRecordFields() throws NoSuchFieldException, IllegalAccessException, ClassNotFoundException {
    Person person = new Person("A", 26);

    Field field = Person.class.getDeclaredField("name");
    field.setAccessible(true); // Noncompliant
    field.set(person, "B"); // Noncompliant

    Person.class.getField("name").setAccessible(true); // Noncompliant
    Person.class.getField("name").set(person, "B"); // Noncompliant


    Field[] fields = Person.class.getDeclaredFields();
    fields[0].setAccessible(true); // Noncompliant
    fields[0].set(person, "B"); // Noncompliant
    fields[0].setAccessible(true); // Noncompliant
    fields[0].set(person, "B"); // Noncompliant

    Person.class.getFields()[0].setAccessible(true); // Noncompliant
    Person.class.getFields()[0].set(person, "B"); // Noncompliant
    Person.class.getDeclaredFields()[0].setAccessible(true); // Noncompliant
    Person.class.getDeclaredFields()[0].set(person, "B"); // Noncompliant

    Field someMagicField = getAField();
    someMagicField.setAccessible(true); // Noncompliant
    someMagicField.set(person, "B"); // Noncompliant

    getAField().setAccessible(true); // Noncompliant
    getAField().set(person, "B"); // Noncompliant

    Field nullInitializedField = null;
    nullInitializedField.setAccessible(true); // Noncompliant
    nullInitializedField.set(person, "B"); // Noncompliant

    Field fieldFromDynamicallyLoadedClass = Class.forName("org.sonar.some.package.SomeClass").getDeclaredField("");
    fieldFromDynamicallyLoadedClass.setAccessible(true); // Noncompliant
    fieldFromDynamicallyLoadedClass.set(person, "B"); // Noncompliant
  }

  Field getAField() {
    return Person.class.getDeclaredFields()[0];
  }
}
