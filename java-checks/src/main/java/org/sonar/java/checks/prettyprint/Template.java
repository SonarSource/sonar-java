package org.sonar.java.checks.prettyprint;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.sonar.java.model.InternalSyntaxToken;
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
import org.sonar.plugins.java.api.tree.Tree;

public final class Template<T extends Tree> {

  private static final int JAVA_VERSION = 21;

  private final T templateTree;
  private final Class<T> clazz;

  public static Template<StatementTree> statementTemplate(String code) {
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
    var templateTree = method.block().body().get(0);
    return new Template<>(templateTree, StatementTree.class);
  }

  public static Template<ExpressionTree> expressionTemplate(String code){
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
    var templateTree = returnStat.expression();
    return new Template<>(templateTree, ExpressionTree.class);
  }

  public static Template<CaseGroupTree> caseGroupTemplate(String code){
    final var WRAPPER =
      // language=java
      """
        class Template {
          static void template(int x){
            switch (x){
              $$$
            }
          }
        }
        """;
    var cu = parse(WRAPPER.replace("$$$", code));
    var clazz = (ClassTree) cu.types().get(0);
    var method = (MethodTree) clazz.members().get(0);
    var switchStat = (SwitchTree) method.block().body().get(0);
    var templateTree = switchStat.cases().get(0);
    return new Template<>(templateTree, CaseGroupTree.class);
  }

  public static InternalSyntaxToken token(String s){
    return new InternalSyntaxToken(0, 0, s, List.of(), false);
  }

  private static CompilationUnitTree parse(String srcCode){
    var parserConfig = JParserConfig.Mode.BATCH.create(new JavaVersionImpl(JAVA_VERSION), List.of());
    return JParser.parse(parserConfig.astParser(), Integer.toString(JAVA_VERSION), "Template.java", srcCode);
  }

  private Template(T templateTree, Class<T> clazz) {
    this.templateTree = templateTree;
    this.clazz = clazz;
  }

  public T apply(Tree... trees){
    var result = SubstitutionVisitor.substitute(templateTree, createSubst(trees));
    return clazz.cast(result);
  }

  private Map<String, Tree> createSubst(Tree[] elems){
    var subst = new HashMap<String, Tree>();
    var idx = 0;
    for (var e : elems) {
      subst.put("$" + idx, e);
      idx += 1;
    }
    return subst;
  }

}
