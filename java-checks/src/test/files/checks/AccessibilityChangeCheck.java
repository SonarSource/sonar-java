class A {
    void makeItPublic(String methodName, java.lang.reflect.AccessibleObject[] arr, boolean someBool) throws NoSuchMethodException {
      this.getClass().getMethod(methodName).setAccessible(true); // Noncompliant {{Make sure that this accessibility update is safe here.}}
      this.getClass().getMethod(methodName).setAccessible(arr, true); // Noncompliant {{Make sure that this accessibility update is safe here.}}
      this.getClass().getMethod(methodName).setAccessible(arr, false); // compliant
      this.getClass().getMethod(methodName).setAccessible(arr, someBool); // compliant
    }
    void setItAnyway(String fieldName, int value) {
      this.getClass().getDeclaredField(fieldName).setInt(this, value); // Noncompliant {{Make sure that this accessibility bypass is safe here.}}
    }
}
