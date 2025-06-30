package checks;

import java.io.IOException;
import java.lang.classfile.ClassFile;
import java.lang.classfile.ClassModel;
import java.lang.classfile.ClassTransform;
import java.lang.classfile.MethodModel;
import java.lang.constant.ClassDesc;
import java.nio.file.Path;
import java.util.Optional;

public class ClassNameInClassTransformCheckSample {
  ClassTransform classTransform = (classBuilder, classElement) -> {
    if (!(classElement instanceof MethodModel methodModel &&
      methodModel.methodName().stringValue().startsWith("debug"))) {
      classBuilder.with(classElement);
    }
  };

  public byte[] transformClassFileNonCompliantDesc(Path path) throws IOException {
    ClassFile classFile = ClassFile.of();
    ClassModel classModel = classFile.parse(path);
    return classFile.transformClass(classModel,
      classModel.thisClass(), // Noncompliant {{Use `transformClass` overload without the class name.}}
//    ^^^^^^^^^^^^^^^^^^^^^^
      classTransform);
  }

  public byte[] transformClassFileNonCompliant(Path path) throws IOException {
    ClassFile classFile = ClassFile.of();
    ClassModel classModel = classFile.parse(path);
    return classFile.transformClass(classModel,
      classModel.thisClass().asSymbol(), // Noncompliant {{Use `transformClass` overload without the class name.}}
//    ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
      classTransform);
  }

  public byte[] transformClassFileNonCompliantInternalName(Path path) throws IOException {
    ClassFile classFile = ClassFile.of();
    ClassModel classModel = classFile.parse(path);
    return classFile.transformClass(classModel,
      ClassDesc.ofInternalName(classModel.thisClass().asInternalName()), // Noncompliant {{Use `transformClass` overload without the class name.}}
//    ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
      classTransform);
  }

  public byte[] transformClassFileNonCompliantName(Path path) throws IOException {
    ClassFile classFile = ClassFile.of();
    ClassModel classModel = classFile.parse(path);
    return classFile.transformClass(classModel,
      ClassDesc.ofInternalName(classModel.thisClass().name().stringValue()), // Noncompliant {{Use `transformClass` overload without the class name.}}
//    ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
      classTransform);
  }

  public byte[] transformClassFileNonCompliantDesc2(Path path) throws IOException {
    ClassFile classFile = ClassFile.of();
    ClassModel classModel = classFile.parse(path);
    return classFile.transformClass(classModel,
      ClassDesc.ofDescriptor(classModel.thisClass().asSymbol().descriptorString()), // Noncompliant {{Use `transformClass` overload without the class name.}}
//    ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
      classTransform);
  }

  public byte[] transformClassFile(Path path) throws IOException {
    ClassFile classFile = ClassFile.of();
    ClassModel classModel = classFile.parse(path);
    return classFile.transformClass(classModel, // Compliant
      classTransform);
  }

  public byte[] transformClassFile_changedName(Path path) throws IOException {
    ClassFile classFile = ClassFile.of();
    ClassModel classModel = classFile.parse(path);
    return classFile.transformClass(classModel,
      ClassDesc.ofInternalName(classModel.thisClass().asInternalName() + "Copy"), // Compliant
      classTransform);
  }

  public byte[] transformClassFile_nonVariable(Path path) throws IOException {
    ClassFile classFile = ClassFile.of();
    Optional<ClassModel> classModel = Optional.of(classFile.parse(path));
    return classFile.transformClass(classModel.get(),
      classModel.get().thisClass(), // False negative: the case where the classModel is not a variable is not handled
      classTransform);
  }
}
