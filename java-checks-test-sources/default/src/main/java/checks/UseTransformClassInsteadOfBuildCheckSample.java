package checks;

import java.io.IOException;
import java.lang.classfile.ClassElement;
import java.lang.classfile.ClassFile;
import java.lang.classfile.ClassModel;
import java.lang.classfile.MethodModel;
import java.nio.file.Files;
import java.nio.file.Path;

public class UseTransformClassInsteadOfBuildCheckSample {
  public static void transformClassFile(Path path) throws IOException {
    ClassFile classFile = ClassFile.of();
    ClassModel classModel = classFile.parse(path);
    byte[] newBytes = classFile.build( // Noncompliant
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
}
