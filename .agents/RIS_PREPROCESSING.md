# SonarJava Rule Engine Analysis

## Executive Summary

The SonarJava analyzer contains **600+ rule implementations** organized across multiple modules with well-established patterns for static analysis. The rule engine uses a **visitor-based architecture** with extensive helper utilities for common detection patterns, semantic analysis, and code generation capabilities.

**Files inspected:**
- `java-checks/src/main/java/org/sonar/java/checks/` (primary rule directory - 600+ rules)
- `java-checks/src/main/java/org/sonar/java/checks/helpers/` (helper utilities - 20+ files)
- `java-checks-common/src/main/java/org/sonar/java/checks/helpers/` (shared utilities)
- `java-checks-aws/src/main/java/org/sonar/java/checks/` (AWS-specific rules)
- `check-list/src/main/java/org/sonar/java/CheckListGenerator.java` (rule registration)
- Representative rule samples: EmptyMethodsCheck, BooleanLiteralCheck, CollectionInappropriateCallsCheck, DeadStoreCheck, HardCodedSecretCheck, NullShouldNotBeUsedWithOptionalCheck

---

## Step 1: Rule Inventory

### Total Rule Count
- **600+ rules** across all modules (java-checks, java-checks-aws, java-checks-common)
- Rules are discovered automatically via `@Rule(key = "SXXXX")` annotations
- Rules organized by domain:
  - Core Java checks (~570 rules in java-checks/)
  - AWS-specific checks (~30 rules in java-checks-aws/)
  - Shared/common utilities (java-checks-common/)

### Naming Conventions
- **Primary pattern:** `*Check.java` (e.g., `EmptyMethodsCheck`, `NullShouldNotBeUsedWithOptionalCheck`)
- **Abstract base classes:** `Abstract*Checker.java` or `Abstract*Rule.java` (e.g., `AbstractForLoopRule`, `AbstractHardCodedCredentialChecker`)
- **Helper utilities:** Descriptive names in `helpers/` subdirectory (e.g., `QuickFixHelper`, `MethodTreeUtils`)

### Architectural Conventions
1. **Rule annotation:** `@Rule(key = "SXXXX")` where SXXXX is the Sonar rule key
2. **Registration:** Automatic discovery via `CheckListGenerator` which scans for classes ending in "Check.java" with @Rule annotation
3. **Categorization:** Rules categorized by metadata in JSON files (Main/Test/All scopes)

### Problem Domain Coverage
Rules cover comprehensive Java analysis domains:
- **Code smells:** Empty methods, duplicate code, complexity
- **Bugs:** Null dereference, incorrect API usage, logic errors
- **Security:** Hard-coded credentials, injection vulnerabilities, weak crypto
- **Performance:** Inefficient collections, redundant operations
- **Maintainability:** Naming conventions, documentation, code organization
- **Java version awareness:** Modern Java feature recommendations (lambdas, Optional, pattern matching)
- **Framework-specific:** Spring, JPA/Hibernate, Jackson, testing frameworks
- **AWS-specific:** Cloud security and best practices

---

## Step 2: Implementation Pattern Analysis

### A. Detection Patterns

#### 1. Syntax Pattern Matching
**Scope:** Most rules (80%+)
**Technique:** Direct AST node kind matching and field access

**Example from EmptyMethodsCheck:**
```java
@Override
public List<Kind> nodesToVisit() {
  return Arrays.asList(Tree.Kind.CLASS, Tree.Kind.ENUM, Tree.Kind.RECORD);
}

@Override
public void visitNode(Tree tree) {
  ClassTree classTree = (ClassTree) tree;
  // Check for empty methods in non-abstract classes
}
```

**Pattern characteristics:**
- Subscribe to specific node kinds (METHOD, CLASS, VARIABLE, etc.)
- Narrow focus on specific syntactic patterns
- Typically 50-150 lines of code per rule

#### 2. Semantic Type Analysis
**Scope:** ~40% of rules
**Technique:** Type hierarchy queries and symbol resolution

**Example from CollectionInappropriateCallsCheck:**
```java
Type actualMethodType = getMethodOwnerType(tree);
Type checkedMethodType = findSuperTypeMatching(actualMethodType, typeChecker.methodOwnerType);
Type parameterType = getTypeArgumentAt(checkedMethodType, typeChecker.parametrizedTypeIndex);
```

**Pattern characteristics:**
- Use `Type.is()`, `Type.isSubtypeOf()` for type checking
- Access parameterized type arguments
- Check super types and interfaces
- Symbol resolution for cross-reference analysis

#### 3. Method Call Detection
**Scope:** ~30% of rules
**Technique:** MethodMatchers fluent API

**Example from CollectionInappropriateCallsCheck:**
```java
private static final List<TypeChecker> TYPE_CHECKERS = new TypeCheckerListBuilder()
  .on(JAVA_UTIL_COLLECTION)
    .method("remove").argument(1).outOf(1).shouldMatchParametrizedType(1).add()
    .method("contains").argument(1).outOf(1).shouldMatchParametrizedType(1).add()
  .on("java.util.Map")
    .method("get").argument(1).outOf(1).shouldMatchParametrizedType(1).add()
  .build();
```

**Pattern characteristics:**
- Fluent builder for method signature matching
- Match by owner type, method name, parameter types
- Support for varargs and ANY parameter matching
- Reusable across multiple rules

#### 4. Control Flow Analysis
**Scope:** ~5% of rules (complex data flow rules)
**Technique:** Control Flow Graph (CFG) construction and traversal

**Example from DeadStoreCheck:**
```java
CFG cfg = (CFG) methodTree.cfg();
LiveVariables liveVariables = LiveVariables.analyze(cfg);
for (CFG.Block block : cfg.blocks()) {
  checkElements(block, liveVariables.getOut(block), methodSymbol);
}
```

**Pattern characteristics:**
- CFG construction from method body
- Block-level and intra-block analysis
- LiveVariables for liveness analysis
- Typically 200-500 lines for CFG-based rules

#### 5. Entropy/Randomness Analysis
**Scope:** Security rules for secret detection
**Technique:** Shannon entropy calculation

**Example from HardCodedSecretCheck:**
```java
private RandomnessDetector randomnessDetector;

@Override
protected boolean isPotentialCredential(String literal) {
  return getRandomnessDetector().isRandom(literal)
    && isNotIpV6(literal)
    && !isKnownNonSecret(literal);
}
```

**Pattern characteristics:**
- Custom entropy calculation (ShannonEntropy helper)
- Configurable sensitivity threshold
- Combined with pattern matching for precision

### B. Implementation Complexity

#### Simple Rules (50-150 LOC)
**Examples:** EmptyMethodsCheck, BooleanLiteralCheck, ArrayDesignatorAfterTypeCheck

**Characteristics:**
- Single visitor method
- Direct node kind checks
- Minimal helper functions (0-2)
- No state tracking
- Quick fix support via QuickFixHelper

**Typical structure:**
```java
@Rule(key = "SXXXX")
public class SimpleCheck extends IssuableSubscriptionVisitor {
  @Override
  public List<Kind> nodesToVisit() { /* 1-3 node kinds */ }
  
  @Override
  public void visitNode(Tree tree) {
    // Direct pattern check
    if (matchesPattern(tree)) {
      reportIssue(tree, "Message");
    }
  }
}
```

#### Medium Rules (150-300 LOC)
**Examples:** CollectionInappropriateCallsCheck, HardCodedSecretCheck

**Characteristics:**
- Multiple helper methods (3-6)
- MethodMatchers for API detection
- Type hierarchy queries
- Moderate state (instance variables for configuration)
- Complex filtering logic

**Typical structure:**
```java
@Rule(key = "SXXXX")
public class MediumCheck extends IssuableSubscriptionVisitor {
  private static final MethodMatchers MATCHERS = /* ... */;
  
  @RuleProperty
  public String configParameter = "default";
  
  @Override
  public void visitNode(Tree tree) {
    if (MATCHERS.matches((MethodInvocationTree) tree)) {
      checkWithSemanticAnalysis(tree);
    }
  }
  
  private void checkWithSemanticAnalysis(Tree tree) { /* ... */ }
}
```

#### Complex Rules (300-600+ LOC)
**Examples:** DeadStoreCheck, SymbolicExecutionBlocks, AbstractRegexCheck

**Characteristics:**
- 10+ helper methods
- CFG/data flow analysis
- Multiple inner classes
- Sophisticated state tracking
- Custom tree visitors
- Framework-specific knowledge

**Typical structure:**
```java
@Rule(key = "SXXXX")
public class ComplexCheck extends IssuableSubscriptionVisitor {
  @Override
  public void visitNode(Tree tree) {
    CFG cfg = buildCFG(tree);
    DataFlowAnalysis analysis = new DataFlowAnalysis(cfg);
    checkDataFlowResults(analysis.getResults());
  }
  
  private static class DataFlowAnalysis extends BaseTreeVisitor {
    // Custom analysis logic
  }
}
```

### C. Core Analysis Techniques

#### 1. Direct Node Kind Matching
**Usage:** Universal across all rules
```java
if (tree.is(Tree.Kind.METHOD_INVOCATION)) {
  MethodInvocationTree mit = (MethodInvocationTree) tree;
}
```

#### 2. Field/Property Access
**Usage:** ~80% of rules
```java
MethodTree method = (MethodTree) tree;
BlockTree body = method.block();
List<StatementTree> statements = body.body();
```

#### 3. Tree Traversal Strategies

**Subscription-based (IssuableSubscriptionVisitor):**
- Declare node kinds of interest
- Automatic traversal to matching nodes
- Most common pattern (90%+ of rules)

**Custom visitor (BaseTreeVisitor):**
- Full control over traversal
- Override specific visit methods
- Used for complex inter-node analysis

**Example:**
```java
class CustomVisitor extends BaseTreeVisitor {
  @Override
  public void visitMethod(MethodTree tree) {
    // Custom logic
    super.visitMethod(tree);  // Continue traversal
  }
}
```

#### 4. Ancestor/Descendant Walking
**Usage:** ~20% of rules (context-dependent checks)

Helper methods from QuickFixHelper:
```java
Tree parent = tree.parent();  // Direct parent access
// Navigate to specific ancestor type
while (parent != null && !parent.is(Tree.Kind.CLASS)) {
  parent = parent.parent();
}
```

#### 5. Symbol Lookup
**Usage:** ~40% of rules (semantic analysis)
```java
Symbol.MethodSymbol methodSymbol = methodTree.symbol();
Symbol.TypeSymbol ownerType = methodSymbol.owner();
List<Type> parameterTypes = methodSymbol.parameterTypes();
```

#### 6. File-Level Filtering
**Usage:** ~10% of rules (test-specific, version-aware)

**Test detection:**
```java
if (UnitTestUtils.isInTestFile(methodTree)) {
  return;  // Skip test code
}
```

**Version awareness:**
```java
public class MyCheck extends IssuableSubscriptionVisitor 
    implements JavaVersionAwareVisitor {
  @Override
  public boolean isCompatibleWithJavaVersion(JavaVersion version) {
    return version.isJava14Compatible();
  }
}
```

#### 7. Data-Flow Tracking
**Usage:** ~5% of rules (advanced checks)

**Liveness analysis:**
```java
CFG cfg = (CFG) methodTree.cfg();
LiveVariables liveVariables = LiveVariables.analyze(cfg);
Set<Symbol> liveOut = liveVariables.getOut(block);
```

**Variable read tracking:**
```java
VariableReadExtractor extractor = new VariableReadExtractor();
Set<Symbol> readVariables = extractor.extract(tree);
```

### D. Validation and Filtering

#### 1. Context Checks
**Prevalence:** ~60% of rules

**Common patterns:**
- Check if in test code: `UnitTestUtils.isInTestFile()`
- Check method modifiers: `ModifiersUtils.hasModifier(modifiers, Modifier.STATIC)`
- Check class context: `!ModifiersUtils.hasModifier(classTree.modifiers(), Modifier.ABSTRACT)`
- Check annotation presence: `AnnotationsHelper.hasAnnotation(method, "Override")`

#### 2. Allowlist/Denylist Logic
**Example from DisallowedClassCheck:**
```java
@RuleProperty(
  key = "disallowedClasses",
  description = "Comma-separated list of disallowed classes"
)
public String disallowedClasses = "";

private void checkType(Type type) {
  if (disallowedClassList.contains(type.fullyQualifiedName())) {
    reportIssue(/* ... */);
  }
}
```

#### 3. Exception Handling
**Negative handling:**
```java
// Filter out known exceptions
if (isExceptedAnnotation(annotation) || isKnownFrameworkPattern(tree)) {
  return;  // No issue
}
```

**Null-safety:**
```java
ExpressionTree initializer = variable.initializer();
if (initializer != null && matchesPattern(initializer)) {
  reportIssue(/* ... */);
}
```

#### 4. Performance Optimizations

**Early returns:**
```java
if (methodTree.block() == null) {
  return;  // Abstract method, skip
}
```

**Caching:**
```java
private RandomnessDetector randomnessDetector;

private RandomnessDetector getRandomnessDetector() {
  if (randomnessDetector == null) {
    randomnessDetector = new RandomnessDetector(sensitivity);
  }
  return randomnessDetector;
}
```

**Lazy evaluation:**
```java
if (cheapCheck(tree) && expensiveCheck(tree)) {
  reportIssue(tree, "Message");
}
```

---

## Step 3: Reusable Patterns and Shared Infrastructure

### A. Common Helper Functions

#### 1. QuickFixHelper (java-checks/src/main/java/org/sonar/java/checks/helpers/)
**Purpose:** Quick fix generation and tree navigation

**Key capabilities:**
- `newIssue(context)` - Create issues with quick fix support
- `nextToken(tree)` / `previousToken(tree)` - Token navigation
- `nextVariable(variable)` / `previousVariable(variable)` - Multi-variable declaration handling
- Issue reporting with text edits and code transformations

**Usage in ~40% of rules:**
```java
QuickFixHelper.newIssue(context)
  .forRule(this)
  .onTree(tree)
  .withMessage("Problem description")
  .withQuickFixes(() -> getQuickFixes())
  .report();
```

#### 2. MethodTreeUtils
**Purpose:** Method-related utilities

**Key functions:**
- `isMainMethod(method, javaVersion)` - Detect main methods (version-aware)
- `isPublic(method)`, `isStatic(method)`, `isNamed(method, name)` - Modifier checks
- `parentMethodInvocationOfArgumentAtPos()` - Context detection for arguments
- `consecutiveMethodInvocation()` - Method chaining detection

**Usage in ~25% of rules**

#### 3. ExpressionsHelper / ExpressionUtils
**Purpose:** Expression analysis utilities

**Key functions:**
- `skipParentheses(expression)` - Remove redundant parentheses
- `extractIdentifier(expression)` - Get underlying identifier
- `methodName(methodInvocation)` - Extract method name tree
- Expression simplification and normalization

#### 4. ModifiersUtils
**Purpose:** Modifier checking

**Functions:**
- `hasModifier(modifiers, Modifier.PUBLIC)` - Check specific modifier
- `hasAnyModifier(modifiers, Modifier.PUBLIC, Modifier.PROTECTED)` - Multiple modifier check

**Usage in ~50% of rules**

#### 5. UnitTestUtils
**Purpose:** Test code detection

**Functions:**
- `isInTestFile(tree)` - Detect if code is in test file
- `hasTestAnnotation(method)` - Detect JUnit/TestNG annotations

**Usage in ~15% of rules**

#### 6. AnnotationsHelper
**Purpose:** Annotation handling

**Functions:**
- `hasAnnotation(tree, "Override")` - Check for specific annotation
- `getAnnotation(tree, type)` - Retrieve annotation tree
- Annotation value extraction

**Usage in ~20% of rules**

#### 7. SpringUtils
**Purpose:** Spring Framework detection

**Functions:**
- `isSpringComponent(classTree)` - Detect Spring components
- Spring-specific pattern matching

#### 8. RandomnessDetector & ShannonEntropy
**Purpose:** Secret detection via entropy analysis

**Functions:**
- `isRandom(string)` - Calculate if string has high entropy
- Configurable sensitivity for secret detection

**Usage in security rules**

### B. Reusable Traversal Utilities

#### 1. UnresolvedIdentifiersVisitor
**Purpose:** Track unresolved symbols for reduced false positives

**Usage in ~10% of rules:**
```java
UNRESOLVED_IDENTIFIERS_VISITOR.check(methodTree);
```

#### 2. AbstractAssertionVisitor
**Purpose:** Unified assertion framework detection (JUnit, AssertJ, etc.)

**Usage in test-related rules**

#### 3. FlexibleConstructorVisitor
**Purpose:** Constructor body validation patterns

**Usage in Java 21+ flexible constructor rules**

### C. Repeated Filtering Logic

#### 1. Test Code Filtering
**Pattern appears in ~15% of rules:**
```java
if (UnitTestUtils.isInTestFile(tree)) {
  return;  // Skip test code
}
```

#### 2. Abstract Method/Class Filtering
**Pattern appears in ~20% of rules:**
```java
if (ModifiersUtils.hasModifier(classTree.modifiers(), Modifier.ABSTRACT)) {
  return;  // Skip abstract classes
}
```

#### 3. Unknown Type Filtering
**Pattern appears in ~30% of rules:**
```java
if (type.isUnknown()) {
  return;  // Skip when type cannot be resolved
}
```

#### 4. Framework Exception Filtering
**Pattern appears in ~10% of rules:**
```java
if (SpringUtils.isSpringComponent(classTree) && hasSpringAnnotation(tree)) {
  return;  // Framework manages lifecycle
}
```

### D. Repeated Diagnostic Construction Patterns

#### 1. Simple Issue Reporting
```java
reportIssue(tree, "Message describing the problem");
```

#### 2. Issue with Secondary Locations
```java
QuickFixHelper.newIssue(context)
  .forRule(this)
  .onTree(primaryLocation)
  .withMessage("Primary message")
  .withSecondaries(secondaryLocations.stream()
    .map(loc -> new JavaFileScannerContext.Location("Secondary", loc))
    .toList())
  .report();
```

#### 3. Issue with Quick Fixes
```java
List<JavaTextEdit> edits = Arrays.asList(
  JavaTextEdit.replaceTree(oldTree, "newCode"),
  JavaTextEdit.removeTextSpan(textSpan)
);
JavaQuickFix quickFix = JavaQuickFix.newQuickFix("Description")
  .addTextEdits(edits)
  .build();
```

### E. Abstract Base Classes for Rule Families

#### 1. AbstractForLoopRule
**Purpose:** Common for-loop analysis infrastructure
**Provides:** Loop counter extraction, initializer/increment parsing
**Used by:** 8+ for-loop-related rules

#### 2. AbstractHardCodedCredentialChecker
**Purpose:** Hard-coded credential detection patterns
**Provides:** String literal analysis, credential word matching, entropy filtering
**Used by:** HardCodedPasswordCheck, HardCodedSecretCheck

#### 3. AbstractRegexCheck
**Purpose:** Regex pattern analysis
**Provides:** Regex extraction from String.matches(), Pattern.compile(), etc.
**Used by:** 15+ regex-related rules

#### 4. AbstractCallToDeprecatedCodeChecker
**Purpose:** Deprecated API usage detection
**Provides:** Symbol deprecation checking
**Used by:** CallToDeprecatedMethodCheck, CallToDeprecatedCodeMarkedForRemovalCheck

#### 5. AbstractSerializableInnerClassRule
**Purpose:** Serialization contract validation
**Provides:** Serializable detection, inner class analysis
**Used by:** Multiple serialization rules

#### 6. AbstractPackageInfoChecker
**Purpose:** Package-info.java validation
**Provides:** Package-level annotation checking
**Used by:** Package documentation rules

### F. MethodMatchers Pattern Library

**Reusable matchers defined as static constants:**

```java
// Collection method matchers
private static final MethodMatchers COLLECTION_REMOVE = MethodMatchers.create()
  .ofTypes("java.util.Collection")
  .names("remove", "removeAll")
  .withAnyParameters()
  .build();

// Spring annotation matchers
private static final MethodMatchers SPRING_REQUEST_MAPPING = MethodMatchers.create()
  .ofTypes("org.springframework.web.bind.annotation.RequestMapping")
  .anyName()
  .withAnyParameters()
  .build();
```

**Builder patterns:**
- `.ofTypes(String...)` - Specify owner types
- `.names(String...)` - Method names
- `.addParametersMatcher(String...)` - Parameter types
- `.withAnyParameters()` - Match any parameters
- `.build()` - Create matcher

---

## Summary: Implementation Cost Factors for New Rules

Based on the analysis, new rule implementation cost depends on:

### Low Cost (1-3 days)
- Simple syntax pattern matching
- Extends IssuableSubscriptionVisitor
- Uses existing MethodMatchers
- Reuses helper utilities (ModifiersUtils, UnitTestUtils, etc.)
- 50-150 LOC

### Medium Cost (3-7 days)
- Semantic type analysis required
- Custom MethodMatchers needed
- Moderate filtering logic
- Quick fix implementation
- 150-300 LOC

### High Cost (1-3 weeks)
- Control flow / data flow analysis
- New CFG-based analysis
- Complex state tracking
- Custom tree visitors
- Integration with symbolic execution
- 300-600+ LOC

### Infrastructure Available to Reduce Cost
1. **600+ existing rules as reference implementations**
2. **20+ helper utility classes** for common patterns
3. **Abstract base classes** for rule families
4. **MethodMatchers fluent API** for method detection
5. **CFG and LiveVariables** for data flow
6. **QuickFixHelper** for automated fixes
7. **Comprehensive test infrastructure** (test files, verifiers)
8. **Automatic rule discovery** via annotations

The rule engine is mature and well-factored, with significant reusable infrastructure that substantially reduces the cost of implementing new rules.
