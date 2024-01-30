# Import of Eclipse JDT Core

## License

To be compliant with usage and modification of the https://github.com/eclipse-jdt/eclipse.jdt.core source code, 
this folder, and its subdirectory, does not use its GitHub repository's `GNU LESSER GENERAL PUBLIC-3.0 License`
but the `Eclipse Public License - v 2.0` see [LICENSE](LICENSE).

## Modifications

### org.eclipse.jdt:org.eclipse.jdt.core from 3.33.0 to 3.36.0

The change to the 3.36.0 version introduce a bug which prevent analysis of files when there are semantic problems.
The `ProblemReporter` could have a null `referenceContext` during the resolution of `unknown types` which produces
`AbortCompilation` exception, instead of adding the semantic problems to the compilation result.
The logic in `ProblemReporter` is sometimes wrong, and it wrongly set `referenceContext` to null.
For the Java analyzer, it's an important problem because even when the semantic is not 100% resolved, we still want
to analyze the source code. And this bug also silently prevent the following files in the same compilation batch to be analyzed.

Modified source code branch:
* https://github.com/eclipse-jdt/eclipse.jdt.core/blob/R4_30/org.eclipse.jdt.core.compiler.batch/

Modified files:
* https://github.com/eclipse-jdt/eclipse.jdt.core/blob/R4_30/org.eclipse.jdt.core.compiler.batch/src/org/eclipse/jdt/internal/compiler/problem/ProblemHandler.java

`@@ -131,12 +131,7 @@ public void handle(`
```diff
                 // Error is not to be exposed, but clients may need still notification as to whether there are silently-ignored-errors.
                 // if no reference context, we need to abort from the current compilation process
                 if (referenceContext == null) {
-                        if ((severity & ProblemSeverities.Error) != 0) { // non reportable error is fatal
-                                CategorizedProblem problem = this.createProblem(null, problemId, problemArguments, elaborationId, messageArguments, severity, 0, 0, 0, 0);
-                                throw new AbortCompilation(null, problem);
-                        } else {
-                                return; // ignore non reportable warning
-                        }
+                        return; // ignore non reportable problems
                 }
                 if (mandatory)
                         referenceContext.tagAsHavingIgnoredMandatoryErrors(problemId);
```

`@@ -156,12 +151,7 @@ public void handle(`
```diff
        // if no reference context, we need to abort from the current compilation process
        if (referenceContext == null) {
-               if ((severity & ProblemSeverities.Error) != 0) { // non reportable error is fatal
-                       CategorizedProblem problem = this.createProblem(null, problemId, problemArguments, elaborationId, messageArguments, severity, 0, 0, 0, 0);
-                       throw new AbortCompilation(null, problem);
-               } else {
-                       return; // ignore non reportable warning
-               }
+               return; // ignore non reportable problems
        }

        int[] lineEnds;
```

* https://github.com/eclipse-jdt/eclipse.jdt.core/blob/R4_30/org.eclipse.jdt.core.compiler.batch/src/org/eclipse/jdt/internal/compiler/problem/ProblemReporter.java

`@@ -12499,6 +12499,6 @@ public boolean scheduleProblemForContext(Runnable problemComputation) {`
```diff
*/
@Override
public void close() {
-       this.referenceContext = null;
+       // Intentionally removed "this.referenceContext = null" which is called by mistake by ECJ 3.36.0
}
```
