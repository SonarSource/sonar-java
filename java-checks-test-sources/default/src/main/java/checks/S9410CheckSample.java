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
    Child() { super(1); }
  }

  interface Greeter {
    String greet();
  }

  record Point(int x, int y) { }

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
}
