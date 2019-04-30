package org.sonar.java.ecj;

import org.eclipse.jdt.core.dom.Modifier;
import org.sonar.java.resolve.Flags;
import org.sonar.java.resolve.JavaSymbol;
import org.sonar.plugins.java.api.semantic.Symbol;

public final class MethodSymbolUtils {

  private MethodSymbolUtils() {
  }

  /**
   * {@link JavaSymbol.MethodJavaSymbol#defaultValue()}
   */
  public static Object defaultValue(Symbol.MethodSymbol m) {
    if (!EcjParser.ENABLED) {
      return ((JavaSymbol.MethodJavaSymbol) m).defaultValue();
    }

    return ((EMethodSymbol) m).binding.getDefaultValue();
  }

  /**
   * {@link JavaSymbol.MethodJavaSymbol#isParametrized()}
   */
  public static boolean isParametrized(Symbol.MethodSymbol m) {
    if (!EcjParser.ENABLED) {
      return ((JavaSymbol.MethodJavaSymbol) m).isParametrized();
    }

    return ((EMethodSymbol) m).binding.isParameterizedMethod();
  }

  /**
   * {@link JavaSymbol.MethodJavaSymbol#isVarArgs()}
   */
  public static boolean isVarArgs(Symbol.MethodSymbol m) {
    if (!EcjParser.ENABLED) {
      return ((JavaSymbol.MethodJavaSymbol) m).isVarArgs();
    }

    return ((EMethodSymbol) m).binding.isVarargs();
  }

  /**
   * {@link JavaSymbol.MethodJavaSymbol#isDefault()}
   */
  public static boolean isDefault(Symbol.MethodSymbol m) {
    if (!EcjParser.ENABLED) {
      return ((JavaSymbol.MethodJavaSymbol) m).isDefault();
    }

    return Modifier.isDefault(((EMethodSymbol) m).binding.getModifiers());
  }

  public static boolean isSynchronized(Symbol.MethodSymbol m) {
    if (!EcjParser.ENABLED) {
      return Flags.isFlagged(((JavaSymbol.MethodJavaSymbol) m).flags(), Flags.SYNCHRONIZED);
    }

    return Modifier.isSynchronized(((EMethodSymbol) m).binding.getModifiers());
  }

  /**
   * {@link JavaSymbol.MethodJavaSymbol#isOverridable()}
   */
  public static boolean isOverridable(Symbol.MethodSymbol m) {
    return !(m.isPrivate() || m.isStatic() || m.isFinal() || m.owner().isFinal());
  }

}
