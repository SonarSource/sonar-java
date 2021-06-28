package checks;

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
}
