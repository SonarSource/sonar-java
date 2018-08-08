class A {
    void makeItPublic(String methodName) throws NoSuchMethodException {
      this.getClass().getMethod(methodName).setAccessible(true); // Noncompliant {{Remove this accessibility update.}}
    }
    void setItAnyway(String fieldName, int value) {
      this.getClass().getDeclaredField(fieldName).setInt(this, value); // Noncompliant {{Remove this accessibility bypass.}}
    }
}
