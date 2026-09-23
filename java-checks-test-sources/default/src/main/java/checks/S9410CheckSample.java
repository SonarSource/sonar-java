package checks;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.invoke.VarHandle;
import java.util.List;

class S9410CheckSample {

  static class UserService {
    int updateUser(int userId, String name) { return userId; }
    static String format(String value) { return value.trim(); }
    void noArguments() { }
    void takesArray(String[] values) { }
    UserService(int id) { }
    int count;
    static String version;
  }

  static class Child extends UserService {
    int childField;
    Child() { super(1); }
  }

  interface Greeter {
    String greet();
  }

  record Point(int x, int y) { }

  static class GenericHolder<T> {
    T value;
    T getValue() { return value; }
    void setValue(T v) { this.value = v; }
  }

  static class ParentWithField {
    String parentField;
    static int staticParentField;
  }

  static class ChildWithOwnField extends ParentWithField {
    int childOnly;
  }

  static void verify(MethodHandles.Lookup lookup) throws Throwable {
    MethodHandle valid = lookup.findVirtual(UserService.class, "updateUser", MethodType.methodType(int.class, int.class, String.class));
    MethodHandle wrongParameter = lookup.findVirtual(UserService.class, "updateUser", MethodType.methodType(int.class, String.class, String.class)); // Noncompliant {{Use a type signature matching the target method.}}
    MethodHandle wrongReturn = lookup.findVirtual(UserService.class, "updateUser", MethodType.methodType(void.class, int.class, String.class)); // Noncompliant
    MethodHandle wrongArity = lookup.findVirtual(UserService.class, "updateUser", MethodType.methodType(int.class, int.class)); // Noncompliant
    MethodHandle validStatic = lookup.findStatic(UserService.class, "format", MethodType.methodType(String.class, String.class));
    MethodHandle wrongStaticKind = lookup.findVirtual(UserService.class, "format", MethodType.methodType(String.class, String.class)); // Noncompliant
    MethodHandle validArray = lookup.findVirtual(UserService.class, "takesArray", MethodType.methodType(void.class, String[].class));
    MethodHandle validEmpty = lookup.findVirtual(UserService.class, "noArguments", MethodType.methodType(void.class));
    MethodHandle validConstructor = lookup.findConstructor(UserService.class, MethodType.methodType(void.class, int.class));
    MethodHandle wrongConstructor = lookup.findConstructor(UserService.class, MethodType.methodType(void.class, String.class)); // Noncompliant

    VarHandle validField = lookup.findVarHandle(UserService.class, "count", int.class);
    VarHandle wrongField = lookup.findVarHandle(UserService.class, "count", Integer.class); // Noncompliant {{Use the declared type of the target field.}}
    VarHandle validStaticField = lookup.findStaticVarHandle(UserService.class, "version", String.class);
    VarHandle wrongStaticField = lookup.findStaticVarHandle(UserService.class, "version", Object.class); // Noncompliant
    String fieldName = "count";
    lookup.findVarHandle(UserService.class, fieldName, String.class);
    MethodType dynamicType = MethodType.methodType(Object.class, String.class);
    lookup.findVirtual(UserService.class, "updateUser", dynamicType);

    // Inherited method lookups (should not raise issues)
    MethodHandle inheritedHashCode = lookup.findVirtual(UserService.class, "hashCode", MethodType.methodType(int.class));
    MethodHandle inheritedToString = lookup.findVirtual(UserService.class, "toString", MethodType.methodType(String.class));
    MethodHandle childInherited = lookup.findVirtual(Child.class, "updateUser", MethodType.methodType(int.class, int.class, String.class));

    // Generic type lookups (should not raise issues due to type erasure)
    MethodHandle listGet = lookup.findVirtual(List.class, "get", MethodType.methodType(Object.class, int.class));
    MethodHandle listAdd = lookup.findVirtual(List.class, "add", MethodType.methodType(boolean.class, Object.class));

    // Interface targets with Object methods (should not raise issues)
    MethodHandle greeterToString = lookup.findVirtual(Greeter.class, "toString", MethodType.methodType(String.class));
    MethodHandle greeterHashCode = lookup.findVirtual(Greeter.class, "hashCode", MethodType.methodType(int.class));
    MethodHandle greeterEquals = lookup.findVirtual(Greeter.class, "equals", MethodType.methodType(boolean.class, Object.class));
    MethodHandle greeterGreet = lookup.findVirtual(Greeter.class, "greet", MethodType.methodType(String.class));
    MethodHandle greeterWrongSig = lookup.findVirtual(Greeter.class, "greet", MethodType.methodType(int.class)); // Noncompliant

    // Record with same-named fields and accessor methods
    MethodHandle pointX = lookup.findVirtual(Point.class, "x", MethodType.methodType(int.class));
    MethodHandle pointWrongX = lookup.findVirtual(Point.class, "x", MethodType.methodType(String.class)); // Noncompliant

    // findSpecial lookup
    MethodHandle specialCall = lookup.findSpecial(UserService.class, "updateUser",
      MethodType.methodType(int.class, int.class, String.class), UserService.class);
    MethodHandle wrongSpecial = lookup.findSpecial(UserService.class, "updateUser",
      MethodType.methodType(void.class, int.class, String.class), UserService.class); // Noncompliant

    // Non-static field with findStaticVarHandle (should raise issue)
    VarHandle wrongStaticKindField = lookup.findStaticVarHandle(UserService.class, "count", int.class); // Noncompliant

    // Static field with findVarHandle (should raise issue)
    VarHandle wrongInstanceKindField = lookup.findVarHandle(UserService.class, "version", String.class); // Noncompliant
  }

  static void edgeCases(MethodHandles.Lookup lookup) throws Throwable {
    Class<?> clazz = UserService.class;

    // Non-class-literal first argument — ignored, covers classLiteralType null branch
    lookup.findVirtual(clazz, "updateUser", MethodType.methodType(int.class, int.class, String.class));

    // Non-constant string name — ignored, covers memberName null branch
    String methodName = getMethodName();
    lookup.findVirtual(UserService.class, methodName, MethodType.methodType(int.class, int.class, String.class));

    // Non-class-literal return type in methodType — ignored, covers returnType null in methodSignature
    Class<?> returnClazz = int.class;
    lookup.findVirtual(UserService.class, "updateUser", MethodType.methodType(returnClazz, int.class, String.class));

    // Non-class-literal parameter type in methodType — ignored, covers parameter null in methodSignature
    Class<?> paramClazz = int.class;
    lookup.findVirtual(UserService.class, "updateUser", MethodType.methodType(int.class, paramClazz, String.class));

    // Non-class-literal type argument for field — ignored, covers requestedType null in checkFieldLookup
    Class<?> fieldType = int.class;
    lookup.findVarHandle(UserService.class, "count", fieldType);

    // Non-class-literal target for field — ignored, covers targetType null in checkFieldLookup
    lookup.findVarHandle(clazz, "count", int.class);

    // Non-constant name for field — ignored, covers memberName null in checkFieldLookup
    String fName = getMethodName();
    lookup.findVarHandle(UserService.class, fName, int.class);

    // Generic type with type variable return — compliant (covers typeVar branch in sameErasure)
    MethodHandle genericGet = lookup.findVirtual(GenericHolder.class, "getValue", MethodType.methodType(Object.class));
    MethodHandle genericSet = lookup.findVirtual(GenericHolder.class, "setValue", MethodType.methodType(void.class, Object.class));

    // Field inherited from parent — compliant (covers field supertype traversal)
    VarHandle inheritedField = lookup.findVarHandle(Child.class, "count", int.class);

    // Non-MethodInvocationTree as methodType argument — ignored, covers methodSignature null branch
    lookup.findVirtual(UserService.class, "updateUser", dynamicMethodType());

    // findConstructor with non-void return type — noncompliant (constructor requires void return)
    MethodHandle badCtorReturn = lookup.findConstructor(UserService.class, MethodType.methodType(int.class, int.class)); // Noncompliant

    // findSpecial with non-existent method name — noncompliant
    MethodHandle badSpecial = lookup.findSpecial(UserService.class, "nonExistent",
      MethodType.methodType(void.class), UserService.class); // Noncompliant

    // Wrong number of arguments for findConstructor — ignored (covers args.size() != 2)
    lookup.findConstructor(UserService.class, MethodType.methodType(void.class, int.class), UserService.class);

    // Wrong number of arguments for findVirtual — ignored (covers args.size() != 3)
    lookup.findVirtual(UserService.class, "updateUser");

    // Wrong number of arguments for findVarHandle — ignored (covers args.size() != 3)
    lookup.findVarHandle(UserService.class, "count");

    // MethodType.methodType() with no arguments — ignored (covers arguments.isEmpty() in methodSignature)
    lookup.findVirtual(UserService.class, "noArguments", MethodType.methodType());

    // Wrong field type on inherited field — noncompliant (covers field inherited from parent miss)
    VarHandle wrongInheritedField = lookup.findVarHandle(Child.class, "count", String.class); // Noncompliant

    // Lookup of non-existent field on child (only exists in parent) — noncompliant static mismatch
    VarHandle wrongStaticInherited = lookup.findStaticVarHandle(Child.class, "count", int.class); // Noncompliant

    // Field inherited from parent with correct type — compliant (covers findField supertype match)
    VarHandle parentFieldOk = lookup.findVarHandle(ChildWithOwnField.class, "parentField", String.class);

    // Field inherited from parent with wrong type — noncompliant
    VarHandle parentFieldWrong = lookup.findVarHandle(ChildWithOwnField.class, "parentField", int.class); // Noncompliant

    // Static field inherited from parent — compliant
    VarHandle staticParentOk = lookup.findStaticVarHandle(ChildWithOwnField.class, "staticParentField", int.class);

    // Null target type for findConstructor — ignored (covers targetType == null for constructor)
    lookup.findConstructor(clazz, MethodType.methodType(void.class));

    // Interface target with non-existent method — noncompliant
    MethodHandle greeterNoMethod = lookup.findVirtual(Greeter.class, "nonExistent", MethodType.methodType(void.class)); // Noncompliant

    // Lookup of generic field — compliant (covers typeVar in sameErasure for fields)
    VarHandle genericField = lookup.findVarHandle(GenericHolder.class, "value", Object.class);
  }

  private static String getMethodName() {
    return "updateUser";
  }

  private static MethodType dynamicMethodType() {
    return MethodType.methodType(int.class, int.class, String.class);
  }
}
