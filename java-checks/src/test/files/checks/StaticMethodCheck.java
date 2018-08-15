package foo;

import java.io.IOException;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.Map;
import java.util.HashMap;

class Utilities {
  private static String magicWord = "magic";
  private static String string = magicWord; // coverage
  private String otherWord = "other";

  public Utilities() {
  }
  
  private void registerPrimitives(final boolean type) {
    register(Boolean.TYPE, new Toto());
  }
  
  private void register(final Class<?> clazz, final Object converter) {
    otherWord = "";
  }
  
  private String getMagicWord() { // Noncompliant [[sc=18;ec=30]] {{Make "getMagicWord" a "static" method.}}
    return magicWord;
  }
  private static String getMagicWordOK() {
    return magicWord;
  }

  private void setMagicWord(String value) { // Noncompliant {{Make "setMagicWord" a "static" method.}}
    magicWord = value;
  }
  private static void setMagicWordOK(String value) {
    magicWord = value;
  }

  private String getClassName() {
    return getClass().getSimpleName();
  }
  
  private void checkClassLoader() throws IllegalArgumentException {
    if (getClass().getClassLoader() != null) {
      throw new IllegalArgumentException ("invalid address type");
  }
  }

  private String getOtherWord() {
    return otherWord;
  }

  private int getOtherWordLength() {
    return otherWord.length();
  }
  private String getOtherWord2() {
    return this.otherWord;
  }
  private String getOtherWord3() {
    return super.toString();
  }

  private void setOtherWord(String value) {
    otherWord = value;
    // coverage
    otherWord = value;
  }
  
  private int useOnlyArguments(int a, int b) {  // Noncompliant
    return a + b;
  }
  
  private String methodOnlyOnArgument(Object obj) {  // Noncompliant
    return (obj == null ? null : obj.toString());
  }
  
  private String attributeOnArgument(Utilities obj) {  // Noncompliant
    return obj.otherWord;
  }

  class Inner {
    public Inner(String a, String b) {
    }

    public final String getMagicWord() {
      return "a";
    }
    
    public String getOuterOtherWord() {
      return Utilities.this.getOtherWord();
    }
  }

  static class Nested {
    private String getAWord() { // Noncompliant {{Make "getAWord" a "static" method.}}
      return "a";
    }
  }

  public void publicMethod() {
  }
  
  public int localAccessViasClass() {  // Compliant
    return Integer.valueOf(otherWord);
  }

  private Utilities.Inner createInner() { // Compliant because there is a reference to an inner, non-static class
    return new Utilities.Inner("", "");
  }
  
  private Utilities.Nested createNested() { // Noncompliant
    return new Utilities.Nested();
  }

  private Unknown unknown() { // Compliant because we should not make any decision on an unknown class
    return new Unknown("", "");
  }

  private Map newMap() { // Noncompliant
    return new HashMap();
  }
  
  private static final int BOOLEAN_TYPE = 1;
  
  private void writeAnyClass(final Class<?> clazz) { // Noncompliant
    int primitiveType = 0;
    if (Boolean.TYPE.equals(clazz)) {
        primitiveType = BOOLEAN_TYPE;
    }
  }
  
  private <T> int sizeOfMap(Map<T> map) { // Noncompliant
    return map.size();
  }
  
  private void callMethodOfStaticClass() { // Noncompliant
    new FooBar().myHash();
  }

}

class UtilitiesExtension extends Utilities {
  public UtilitiesExtension() {
  }
  private void method() { // Compliant
    publicMethod();
  }
}

class SerializableExclusions implements Serializable {
  private void writeObject(java.io.ObjectOutputStream out) throws IOException {}

  private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {}

  private void readObjectNoData() throws ObjectStreamException {}

  private void other() {} // Noncompliant

  private void recursive() { // Noncompliant
    recursive();
  }
  
  private void delegateOther() {  // Compliant since other() is not static (although it should...)
    other();
  }


  private void readResolve() throws ObjectStreamException {
    System.out.println("foo");
  }
  private Object writeReplace() throws ObjectStreamException { }
}

static class FooBar {
  enum MyEnum{
    FOO;
  }
  private void plop() { // Noncompliant enum is static and enum constants are static
    Object o = MyEnum.FOO;
  }
  int myHash() {
    return hashCode();
  }
}

static class FooBarQix {
  private int instanceVariable;
  
  public void instanceMethod() {}

  private void foo() { // Compliant: foo cannot be static because it references a non-static method
    new Plop(){
      void plop1(){
        instanceMethod();
      }
    };
  }

  private void init() { // Compliant: foo cannot be static because it references a non-static field
    new Plop(){
      void plop1(){
        instanceVariable = 0;
      }
    };
  }
}

class Plop {
  Plop(){}
  void plop1(){}
}

class SuperClass {
  public int bar;
}

class EnclosingInstance extends SuperClass {

  interface I { boolean gul(); }

  private int foo;

  private void foo1() { // Compliant: use of 'EnclosingInstance.this'
    new I() {
      @Override
      public boolean gul() {
        return EnclosingInstance.this.foo == 0;
      }
    };
  }

  private void foo2() { // Compliant: use of 'EnclosingInstance.super'
    new I() {
      @Override
      public boolean gul() {
        return EnclosingInstance.super.bar == 0;
      }
    };
  }

  private void foo3() { // Compliant: use of 'EnclosingInstance.this' with fully qualified name
    new I() {
      @Override
      public boolean gul() {
        return foo.EnclosingInstance.this.foo == 0;
      }
    };
  }

  private void foo4() { // Compliant: use of 'EnclosingInstance.super' with fully qualified name
    new I() {
      @Override
      public boolean gul() {
        return foo.EnclosingInstance.super.bar == 0;
      }
    };
  }
}
