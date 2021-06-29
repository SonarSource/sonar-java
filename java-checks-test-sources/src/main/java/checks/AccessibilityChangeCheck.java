package checks;

import java.lang.reflect.Field;

class AccessibilityChangeCheck {
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

  void fieldModificationIsAlwaysCompliantForRecords() throws NoSuchFieldException, IllegalAccessException, ClassNotFoundException {
    Person person = new Person("A", 26);

    Field field = Person.class.getDeclaredField("name");
    field.setAccessible(true); // Compliant because reported by S6216
    field.set(person, "B"); // Compliant because reported by S6216

    Person.class.getField("name").setAccessible(true); // Compliant because reported by S6216
    Person.class.getField("name").set(person, "B"); // Compliant because reported by S6216


    Field[] fields = Person.class.getDeclaredFields();
    fields[0].setAccessible(true); // Compliant because reported by S6216
    fields[0].set(person, "B"); // Compliant because reported by S6216
    fields[0].setAccessible(true); // Compliant because reported by S6216
    fields[0].set(person, "B"); // Compliant because reported by S6216

    Person.class.getFields()[0].setAccessible(true); // Compliant because reported by S6216
    Person.class.getFields()[0].set(person, "B"); // Compliant because reported by S6216
    Person.class.getDeclaredFields()[0].setAccessible(true); // Compliant because reported by S6216
    Person.class.getDeclaredFields()[0].set(person, "B"); // Compliant because reported by S6216

    Field someMagicField = getAField();
    someMagicField.setAccessible(true); // Noncompliant FP Not exploring fields retrieved from non standard methods
    someMagicField.set(person, "B"); // Noncompliant FP Not exploring fields retrieved from non standard methods

    getAField().setAccessible(true); // Noncompliant FP Not exploring fields retrieved from non standard methods
    getAField().set(person, "B"); // Noncompliant FP Not exploring fields retrieved from non standard methods

    Field nullInitializedField = null;
    nullInitializedField.setAccessible(true); // Noncompliant FP Not exploring fields retrieved from non standard methods
    nullInitializedField.set(person, "B"); // Noncompliant FP Not exploring fields retrieved from non standard methods

    Field fieldFromDynamicallyLoadedClass = Class.forName("org.sonar.some.package.SomeClass").getDeclaredField("");
    fieldFromDynamicallyLoadedClass.setAccessible(true); // Noncompliant FP Not exploring fields retrieved from non standard methods
    fieldFromDynamicallyLoadedClass.set(person, "B"); // Noncompliant FP Not exploring fields retrieved from non standard methods
  }

  Field getAField() {
    return Person.class.getDeclaredFields()[0];
  }
}
