package checks;

import java.io.IOException;
import java.lang.classfile.ClassFile;
import java.lang.classfile.ClassModel;
import java.lang.classfile.ClassTransform;
import java.lang.classfile.MethodModel;
import java.lang.constant.ClassDesc;
import java.nio.file.Path;

public class ClassNameInClassTransformCheckSample {
  ClassTransform classTransform = (classBuilder, classElement) -> {
    if (!(classElement instanceof MethodModel methodModel &&
      methodModel.methodName().stringValue().startsWith("debug"))) {
      classBuilder.with(classElement);
    }
  };

  public void transformClassFileNonCompliant(Path path) throws IOException {
    ClassFile classFile = ClassFile.of();
    ClassModel classModel = classFile.parse(path);
    byte[] newBytes = classFile.transformClass(classModel,
      classModel.thisClass().asSymbol(), // Noncompliant
//    ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
      classTransform);
  }

  public void transformClassFileNonCompliantName(Path path) throws IOException {
    ClassFile classFile = ClassFile.of();
    ClassModel classModel = classFile.parse(path);
    byte[] newBytes = classFile.transformClass(classModel,
      ClassDesc.ofInternalName(classModel.thisClass().asInternalName()), // Noncompliant
//    ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
      classTransform);
  }

  public void transformClassFile(Path path) throws IOException {
    ClassFile classFile = ClassFile.of();
    ClassModel classModel = classFile.parse(path);
    byte[] newBytes = classFile.transformClass(classModel,
      classTransform);
  }

  // TODO: add checks with:
  // classModel.thisClass().name().stringValue());
  // classModel.thisClass().asInternalName();
  // classModel.thisClass().asSymbol();
  // ClassDesc.ofInternalName(classModel.thisClass().asInternalName()));
  // ClassDesc.ofInternalName(classModel.thisClass().name().stringValue()));
  //   * classModel.thisClass().asSymbol()
  //   * classModel.thisClass().stringValue()
  //   * classModel.thisClass().asClassDesc()
  //   * classModel.thisClass().asSymbolycRef()

}
