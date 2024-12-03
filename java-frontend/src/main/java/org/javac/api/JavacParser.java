package org.javac.api;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.util.JavacTask;
import com.sun.source.util.TaskEvent;
import com.sun.source.util.TaskListener;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.List;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import org.sonar.java.model.JavaTree;

public class JavacParser {

  public JavaTree.CompilationUnitTreeImpl parse(File file) {
    return parse(List.of(file)).get(0);
  }

  public List<JavaTree.CompilationUnitTreeImpl> parse(List<File> files) {
    return parse(files, List.of());
  }

  public JavaTree.CompilationUnitTreeImpl parse(File file, List<String> opts) {
    return parse(List.of(file), opts).get(0);
  }

  public List<JavaTree.CompilationUnitTreeImpl> parse(List<File> files, List<String> opts) {
    // will this fail if the running java is 8 and we try to parse some java21 syntax?
    JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();

    DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
    StandardJavaFileManager fileManager = compiler.getStandardFileManager(diagnostics, null, StandardCharsets.UTF_8);
    var compilationUnits = fileManager.getJavaFileObjectsFromFiles(files);
    JavacTask task = (JavacTask) compiler.getTask(null, fileManager, diagnostics, opts, null, compilationUnits);
    var sonarJavaParser = new SonarJavaParser(task);

    task.setTaskListener(new TaskListener() {
      @Override
      public void finished(TaskEvent e) {
        if (e.getKind() == TaskEvent.Kind.ANALYZE) {
          CompilationUnitTree compilationUnit = e.getCompilationUnit();
          sonarJavaParser.scan(compilationUnit, null);
        }
      }
    });

    task.call();
    return sonarJavaParser.getParsedCUs();
  }

}
