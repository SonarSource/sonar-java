package godin;

import org.sonar.java.resolve.MethodJavaType;
import org.sonar.plugins.java.api.semantic.Symbol.TypeSymbol;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.TypeArguments;
import org.sonar.plugins.java.api.tree.VariableTree;

public final class IgnoreReasons {

  /**
   * I think that either implementation or test is incorrect.
   */
  public static final String INCORRECT = "incorrect";

  /**
   * Require implementation of {@link VariableTree#endToken()}
   */
  public static final String EVariable_endToken = "EVariable.endToken";

  public static final String TREE_SHAPE = "problem with shape of tree: ";

  /**
   * trivias
   */
  public static final String COMMENTS = "comments aka trivias";

  public static final String SYNTAX_ERROR = "syntax error";

  /**
   * Require implementation of {@link TypeSymbol#lookupSymbols(String)}
   */
  public static final String LOOKUP_SYMBOLS = "TypeSymbol.lookupSymbols";

  /**
   * Require implementation of {@link Type#erasure()}
   */
  public static final String TYPE_ERASURE = "Type.erasure";

  /**
   * Cast to {@link org.sonar.java.resolve.JavaSymbol.VariableJavaSymbol}
   */
  public static final String CAST_TO_VariableJavaSymbol = "cast to VariableJavaSymbol";

  /**
   * Cast to {@link org.sonar.java.model.JavaTree}
   */
  public static final String CAST_TO_JavaTree = "cast to JavaTree";

  /**
   * Cast to {@link org.sonar.java.resolve.JavaType}
   */
  public static final String CAST_TO_JavaType = "cast to JavaType";

  /**
   * Cast to {@link org.sonar.java.resolve.SemanticModel}
   */
  public static final String CAST_TO_SemanticModel = "cast to SemanticModel";

  /**
   * Cast to {@link org.sonar.java.resolve.JavaSymbol.MethodJavaSymbol}
   */
  public static final String CAST_TO_MethodJavaSymbol = "cast to MethodJavaSymbol";

  /**
   * Cast to {@link org.sonar.java.resolve.ParametrizedTypeJavaType}
   */
  public static final String CAST_TO_ParametrizedTypeJavaType = "cast to ParametrizedTypeJavaType";

  /**
   * Cast to {@link org.sonar.java.resolve.JavaSymbol.TypeJavaSymbol}
   */
  public static final String CAST_TO_TypeJavaSymbol = "cast to TypeJavaSymbol";

  /**
   * Cast to {@link org.sonar.java.model.InternalSyntaxToken}
   */
  public static final String CAST_TO_InternalSyntaxToken = "cast to InternalSyntaxToken";

  /**
   * Cast to {@link MethodJavaType}
   */
  public static final String CAST_TO_MethodJavaType = "cast to MethodJavaType";

  private IgnoreReasons() {
  }

}
