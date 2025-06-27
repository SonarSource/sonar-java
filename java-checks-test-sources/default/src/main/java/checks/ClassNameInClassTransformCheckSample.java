package checks;

import java.io.IOException;
import java.lang.classfile.ClassFile;
import java.lang.classfile.ClassModel;
import java.lang.classfile.ClassTransform;
import java.lang.classfile.MethodModel;
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

  public void transformClassFile(Path path) throws IOException {
    ClassFile classFile = ClassFile.of();
    ClassModel classModel = classFile.parse(path);
    byte[] newBytes = classFile.transformClass(classModel,
      classTransform);
  }
}
