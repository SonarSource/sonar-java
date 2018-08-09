class A {
    void makeItPublic(String methodName, java.lang.reflect.AccessibleObject[] arr, boolean someBool) throws NoSuchMethodException {
      this.getClass().getMethod(methodName).setAccessible(true); // Noncompliant {{Remove this accessibility update.}}
      this.getClass().getMethod(methodName).setAccessible(arr, true); // Noncompliant {{Remove this accessibility update.}}
      this.getClass().getMethod(methodName).setAccessible(arr, false); // compliant
      this.getClass().getMethod(methodName).setAccessible(arr, someBool); // compliant
    }
    void setItAnyway(String fieldName, int value) {
      this.getClass().getDeclaredField(fieldName).setInt(this, value); // Noncompliant {{Remove this accessibility bypass.}}
    }
}
