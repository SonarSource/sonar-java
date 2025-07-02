package checks;

import java.lang.classfile.ClassBuilder;
import java.lang.constant.ClassDesc;

import static java.lang.classfile.ClassFile.ACC_PUBLIC;
import static java.lang.classfile.ClassFile.ACC_STATIC;
import static java.lang.constant.ConstantDescs.MTD_void;

public class ClassBuilderWithMethodCheckSample {

  private static final String CLASSNAME = "java.io.PrintStream";

  ClassBuilder addMethod(ClassBuilder builder) {
    return builder
      .withMethod("foo", MTD_void, ACC_PUBLIC | ACC_STATIC, methodBuilder -> { // Noncompliant {{Replace call with `ClassBuilder.withMethodBody`.}}
//     ^^^^^^^^^^
        methodBuilder.withCode(codeBuilder -> codeBuilder.getstatic(ClassDesc.of("java.lang.System"), "out", ClassDesc.of(CLASSNAME))
          .ldc("Hello World")
          .invokevirtual(ClassDesc.of(CLASSNAME), "println", MTD_void)
          .return_());
      });
  }

  ClassBuilder addMethodWithoutCurlyBraces(ClassBuilder builder) {
    return builder
      .withMethod("foo", MTD_void, ACC_PUBLIC | ACC_STATIC, methodBuilder -> // Noncompliant {{Replace call with `ClassBuilder.withMethodBody`.}}
//     ^^^^^^^^^^
        methodBuilder.withCode(codeBuilder -> codeBuilder.getstatic(ClassDesc.of("java.lang.System"), "out", ClassDesc.of("java.io.PrintStream"))
          .ldc("Hello World")
          .invokevirtual(ClassDesc.of("java.io.PrintStream"), "println", MTD_void)
          .return_())
      );
  }

  ClassBuilder addMethodCompliant(ClassBuilder builder) {
    return builder
      .withMethod("foo", MTD_void, ACC_PUBLIC | ACC_STATIC, methodBuilder ->
        {
          // Compliant because `withCode` is not the only statement in the lambda
          methodBuilder.constantPool().intEntry(121);
          methodBuilder.withCode(codeBuilder -> codeBuilder.getstatic(ClassDesc.of("java.lang.System"), "out", ClassDesc.of("java.io.PrintStream"))
            .ldc("Hello World")
            .invokevirtual(ClassDesc.of("java.io.PrintStream"), "println", MTD_void)
            .return_());
        });
  }

  ClassBuilder addMethodCompliantWithMethodBody(ClassBuilder builder) {
    return builder
      .withMethodBody("foo", MTD_void, ACC_PUBLIC | ACC_STATIC, codeBuilder -> codeBuilder.getstatic(ClassDesc.of("java.lang.System"), "out", ClassDesc.of("java.io.PrintStream"))
        .ldc("Hello World")
        .invokevirtual(ClassDesc.of("java.io.PrintStream"), "println", MTD_void)
        .return_());
  }
}
