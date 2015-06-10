/**
 * Test MultipleVariableDeclarations as field
 */
public class OneDeclarationPerLineCheck {

  /** Var int with value */
  private int varFieldInt1 = -1, varFieldInt2, varFieldInt3 = 1; // Noncompliant {{Declare "varFieldInt2", "varFieldInt3" on a separate line.}}

  /** Var object without value */
  Object varFieldObject1, varFieldObject2; // Noncompliant {{Declare "varFieldObject2" on a separate line.}}

  /** Var String without value */
  private String varFiledStringOk; // Compliant, only one on the line

  /**
   * Test MultipleVariableDeclarations inside a method
   * @return int
   */
  public int method() {
    int var1 = 0, var2 = -1; // Noncompliant {{Declare "var2" on a separate line.}}
    int varOk = 1; // Compliant, only one on the line
    return varFieldInt1 + varFieldInt2 + varFieldInt3 + var1 + var2 + varOk;
  }

}
