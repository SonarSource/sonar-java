package godin;

import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Symbol.MethodSymbol;
import org.sonar.plugins.java.api.semantic.Symbol.TypeSymbol;
import org.sonar.plugins.java.api.semantic.Type;

public final class IgnoreReasons {

  public static final String COMMENTS = "comments";

  public static final String SYNTAX_ERROR = "syntax error";

  /**
   * Removal of duplicate method allows to pass test.
   */
  public static final String REMOVE_DUPLICATE_METHOD = "duplicate method";

  /**
   * Require implementation of {@link TypeSymbol#lookupSymbols(String)}.
   */
  public static final String LOOKUP_SYMBOLS = "TypeSymbol.lookupSymbols";

  /**
   * Require implementation of {@link Type#erasure()}.
   */
  public static final String TYPE_ERASURE = "Type.erasure";

  /**
   * Require implementation of {@link Symbol#usages()}.
   */
  public static final String SYMBOL_USAGES = "Symbol.usages";

  /**
   * Require implementation of {@link Symbol#declaration()}.
   */
  public static final String SYMBOL_DECLARATION = "Symbol.declaration";

  /**
   * Require implementation of {@link MethodSymbol#overriddenSymbol()}
   */
  public static final String overriddenSymbol = "MethodSymbol.overriddenSymbol";

  public static final String CAST_TO_VariableJavaSymbol = "cast to VariableJavaSymbol";

  public static final String CAST_TO_JavaTree = "cast to JavaTree";

  public static final String CAST_TO_JavaType = "cast to JavaType";
  
  public static final String CAST_TO_SemanticModel = "cast to SemanticModel";

  public static final String CAST_TO_MethodJavaSymbol = "cast to MethodJavaSymbol";

  public static final String CAST_TO_ParametrizedTypeJavaType = "cast to ParametrizedTypeJavaType";

  public static final String CAST_TO_TypeJavaSymbol = "cast to TypeJavaSymbol";

  private IgnoreReasons() {
  }

}
