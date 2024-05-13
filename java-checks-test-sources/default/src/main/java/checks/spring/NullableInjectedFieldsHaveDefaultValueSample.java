package checks.spring;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.springframework.beans.factory.annotation.Value;

public class NullableInjectedFieldsHaveDefaultValueSample {
  @Nullable
  @Value("${my.property}") // Noncompliant  {{Provide a default null value for this field.}} [[sc=3;ec=27;secondary=-1]]
  private String myProperty;

  @Value("${my.property}") // Noncompliant  {{Provide a default null value for this field.}} [[sc=3;ec=27;secondary=+1]]
  @Nullable
  private String myPropertyWithReversedAnnotations;

  @Nullable
  @Value(value = "${my.property}") // Noncompliant [[sc=3;ec=35]]
  private String myOtherProperty;

  @Nullable
  @Value("${my.property}") // Noncompliant [[sc=3;ec=27;quickfixes=literalfix]]
  private String myPropertyToFix;
  // fix@literalfix {{Set null as default value}}
  // edit@literalfix [[sc=10;ec=26]] {{"${my.property:#{null}}"}}

  @Nullable
  @Value(value = "${my.property}") // Noncompliant [[sc=3;ec=35;quickfixes=argumentfix]]
  private String myPropertyToFixOnNamedArgument;
  // fix@argumentfix {{Set null as default value}}
  // edit@argumentfix [[sc=18;ec=34]] {{"${my.property:#{null}}"}}

  public void foo(@Nullable @Value("${my.property}") String argument) { // Noncompliant  {{Provide a default null value for this parameter.}} [[sc=29;ec=53;secondary=+0]]
    /* Do something */
  }

  public void bar(@Value(value = "${my.property}") @Nullable String argument) { // Noncompliant  {{Provide a default null value for this parameter.}} [[sc=19;ec=51;secondary=+0]]
    /* Do something */
  }

  private static final String NON_COMPLIANT_IN_A_CONSTANT = "${non.compliant.constant}";
  //fix@fixStaticConstant {{Set null as default value}}
  //edit@fixStaticConstant [[sl=40;el=40;sc=61;ec=88]] {{"${non.compliant.constant:#{null}}"}}
  @Nullable
  @Value(value = NON_COMPLIANT_IN_A_CONSTANT) // Noncompliant [[sc=3;ec=46;secondary=-1;quickfixes=fixStaticConstant,localFixOnStaticConstant]]
  //fix@localFixOnStaticConstant {{Set null as default value locally}}
  //edit@localFixOnStaticConstant [[sc=18;ec=45]] {{"${non.compliant.constant:#{null}}"}}
  private String myNoncompliantIndirectProperty;

  private final String finalButNotStatic = "${non.compliant.constant}";
  //fix@fixConstant {{Set null as default value}}
  //edit@fixConstant [[sl=49;el=49;sc=44;ec=71]] {{"${non.compliant.constant:#{null}}"}}
  @Nullable
  @Value(finalButNotStatic) // Noncompliant [[sc=3;ec=28;secondary=-1;quickfixes=fixConstant,localFixOnConstant]]
  // fix@localFixOnConstant {{Set null as default value locally}}
  //edit@localFixOnConstant [[sc=10;ec=27]] {{"${non.compliant.constant:#{null}}"}}
  private String myOtherNoncompliantIndirectProperty;

  @Nullable
  @Value(Constants.NON_COMPLIANT_PROPERTY_EXPRESSION_WITHOUT_NULLABLE_DEFAULT) // Noncompliant [[sc=3;ec=79;quickfixes=fixEternal]]
  // fix@fixEternal {{Set null as default value locally}}
  // edit@fixEternal [[sc=10;ec=78]] {{"${myPropertyWithoutDefault:#{null}}"}}
  private String resolvableButForeignConstant;

  private static final String DEFINED_IN_A_CONSTANT = "${my.constant:#{null}}";
  @Nullable
  @Value(value = DEFINED_IN_A_CONSTANT) // Compliant
  private String myCompliantIndirectProperty;

  private String myNonCompliantAttribute;

  @Value("${my.property}") // Noncompliant [[sc=3;ec=27;secondary=+1]]
  public void setMyNonCompliantAttribute(@Nullable String myNonCompliantAttribute) {
    this.myNonCompliantAttribute = myNonCompliantAttribute;
  }

  @Nullable
  @Value(value = DEFINED_IN_A_CONSTANT) // Compliant
  private String myOtherCompliantIndirectProperty;

  @Nullable
  @Value("myProperty") // Compliant for S6816, even if wrong by other rules
  private String myPropertyNotUsingSpEL;

  @Nullable
  @Value("${myProperty") // Compliant for S6816, even if wrong by other rules
  private String myPropertyWithBrokenSpEL;

  @Nullable
  @Value("${my.property:#{null}}") // Compliant, a default value is provided
  private String myCompliantProperty;

  @Value(value = "${my.property}") // Compliant, the field is not nullable
  private String myNonNullProperty;

  @Value("#{null}")
  @Nullable
  private String nullAnyway;


  private String myCompliantAttribute;

  @Value("${my.property:#{null}}") // Compliant
  public void setMyCompliantAttribute(@Nullable String myCompliantAttribute) {
    this.myCompliantAttribute = myCompliantAttribute;
  }

  @CheckForNull
  private String setNothing(@Nullable String useless) {
    return useless;
  }

  @Value("${my.property}") // Compliant not a setter
  private String setNothingWithTwoParameters(@Nullable String a, String b) {
    a = b;
    return a;
  }
}
