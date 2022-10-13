import org.sonarsource.java.buildsrc.tasks.FetchRuleMetadata

/*
  NOTE: Gradle is not used to build SonarJava at this time. It is used for helpful tooling only.
  Use Maven to build SonarJava.
*/

tasks.register<FetchRuleMetadata.FetchSpecificRulesMetadata>("generateRuleMetadata")
tasks.register<FetchRuleMetadata.FetchAllRulesMetadata>("updateRuleMetadata")
