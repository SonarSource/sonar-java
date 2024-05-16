package org.sonar.java.checks.prettyprint;

import java.util.List;
import java.util.Map;
import org.sonar.java.model.JParser;
import org.sonar.java.model.JParserConfig;
import org.sonar.java.model.JavaVersionImpl;
import org.sonar.plugins.java.api.tree.CaseGroupTree;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.ReturnStatementTree;
import org.sonar.plugins.java.api.tree.StatementTree;
import org.sonar.plugins.java.api.tree.SwitchTree;

public final class TemplateParsing {

  private static final int JAVA_VERSION = 21;

  private TemplateParsing() {
  }

  public static StatementTree statementTemplate(String code) {
    final var WRAPPER =
      // language=java
      """
      class Template {
        static void template(){
          $$$;
        }
      }
      """;
    var cu = parse(WRAPPER.replace("$$$", code));
    var clazz = (ClassTree) cu.types().get(0);
    var method = (MethodTree) clazz.members().get(0);
    return method.block().body().get(0);
  }

  public static ExpressionTree expressionTemplate(String code){
    final var WRAPPER =
      // language=java
      """
      class Template {
        static Object template(){
          return $$$;
        }
      }
      """;
    var cu = parse(WRAPPER.replace("$$$", code));
    var clazz = (ClassTree) cu.types().get(0);
    var method = (MethodTree) clazz.members().get(0);
    var returnStat = (ReturnStatementTree) method.block().body().get(0);
    return returnStat.expression();
  }

  public static CaseGroupTree caseGroupTemplate(String code){
    final var WRAPPER =
      // language=java
      """
        class Template {
          static void template(int x){
            switch (x){
              $$$;
            }
          }
        }
        """;
    var cu = parse(WRAPPER.replace("$$$", code));
    var clazz = (ClassTree) cu.types().get(0);
    var method = (MethodTree) clazz.members().get(0);
    var switchStat = (SwitchTree) method.block().body().get(0);
    return switchStat.cases().get(0);
  }

  private static CompilationUnitTree parse(String srcCode){
    var parserConfig = JParserConfig.Mode.BATCH.create(new JavaVersionImpl(JAVA_VERSION), List.of());
    return JParser.parse(parserConfig.astParser(), Integer.toString(JAVA_VERSION), "Template.java", srcCode);
  }

  public static void main(String[] args) {
    var ast = expressionTemplate("foo(1, $t())");
    var repl = expressionTemplate("bar(42)");
    ast.accept(new SubstitutionVisitor(Map.of("$t", repl)));
    var ppsb = new PrettyPrintStringBuilder(FileConfig.DEFAULT_FILE_CONFIG, null, false);
    ast.accept(new Prettyprinter(ppsb));
    System.out.println(ppsb);
  }

}
