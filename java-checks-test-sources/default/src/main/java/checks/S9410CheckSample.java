package checks;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.invoke.VarHandle;

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
    VarHandle validStaticField = MethodHandles.findStaticVarHandle(UserService.class, "version", String.class);
    VarHandle wrongStaticField = MethodHandles.findStaticVarHandle(UserService.class, "version", Object.class); // Noncompliant
    String fieldName = "count";
    lookup.findVarHandle(UserService.class, fieldName, String.class);
    MethodType dynamicType = MethodType.methodType(Object.class, String.class);
    lookup.findVirtual(UserService.class, "updateUser", dynamicType);
  }
}
