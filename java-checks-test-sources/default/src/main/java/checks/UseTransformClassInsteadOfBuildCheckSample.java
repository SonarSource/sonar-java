package checks;

import java.io.IOException;
import java.lang.classfile.ClassElement;
import java.lang.classfile.ClassFile;
import java.lang.classfile.ClassModel;
import java.lang.classfile.MethodModel;
import java.lang.classfile.constantpool.ConstantPool;
import java.lang.classfile.constantpool.ConstantPoolBuilder;
import java.nio.file.Files;
import java.nio.file.Path;

public class UseTransformClassInsteadOfBuildCheckSample {
  public static void transformClassFile(Path path) throws IOException {
    ClassFile classFile = ClassFile.of();
    ClassModel classModel = classFile.parse(path);
    byte[] newBytes = classFile.build( // Noncompliant {{Replace this 'build()' call with 'transformClass()'.}}
      //              ^^^^^^^^^^^^^^^
      classModel.thisClass().asSymbol(), classBuilder -> {
        for (ClassElement classElement : classModel) {
          if (!(classElement instanceof MethodModel methodModel &&
            methodModel.methodName().stringValue().startsWith("debug"))) {
            classBuilder.with(classElement);

          }
        }
      });
    Files.write(path, newBytes);
  }
  
  public static void transformClassFileWithConstantPool(Path path) throws IOException {
    ClassFile classFile = ClassFile.of();
    ClassModel classModel = classFile.parse(path);
    ConstantPoolBuilder constantPoolBuilder = ConstantPoolBuilder.of(classModel);
    byte[] newBytes = classFile.build( // Noncompliant
      //              ^^^^^^^^^^^^^^^
      classModel.thisClass(),
      constantPoolBuilder,
      classBuilder -> {
        for (ClassElement classElement : classModel) {
          if (!(classElement instanceof MethodModel methodModel &&
            methodModel.methodName().stringValue().startsWith("debug"))) {
            classBuilder.with(classElement);

          }
        }
      });
    Files.write(path, newBytes);
  }

  public static void transformClassFileCompliant(Path path) throws IOException {
    ClassFile classFile = ClassFile.of();
    ClassModel classModel = classFile.parse(path);
    byte[] newBytes = classFile.transformClass(
      classModel, (classBuilder, classElement) -> {
        if (!(classElement instanceof MethodModel methodModel &&
          methodModel.methodName().stringValue().startsWith("debug"))) {
          classBuilder.with(classElement);
        }
      });
    Files.write(path, newBytes);
  }

  public static void transformClassFileFalseNegative(Path path) throws IOException {
    var classFile = ClassFile.of();
    var classModel = classFile.parse(path);
    byte[] newBytes = classFile.build(
      // False negative. We don't detect that we could use `transformClass` instead of
      // `build` because the lambda contains several statements.
      classModel.thisClass().asSymbol(), classBuilder -> {
        var methodName = "debug";
        for (ClassElement classElement : classModel) {
          if (!(classElement instanceof MethodModel methodModel &&
            methodModel.methodName().stringValue().startsWith(methodName))) {
            classBuilder.with(classElement);
          }
        }
      });
    Files.write(path, newBytes);
  }
}
