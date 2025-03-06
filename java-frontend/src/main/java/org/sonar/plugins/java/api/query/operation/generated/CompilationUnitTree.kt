package org.sonar.plugins.java.api.query.operation.generated

import org.sonarsource.astquery.operation.builder.SingleBuilder
import org.sonarsource.astquery.operation.builder.OptionalBuilder
import org.sonarsource.astquery.operation.builder.ManyBuilder
import org.sonar.plugins.java.api.tree.CompilationUnitTree
import org.sonarsource.astquery.operation.composite.func
import org.sonarsource.astquery.operation.composite.listFunc
import org.sonarsource.astquery.operation.composite.optFunc

fun  SingleBuilder<out CompilationUnitTree>.eofToken() = func(CompilationUnitTree::eofToken)
fun  OptionalBuilder<out CompilationUnitTree>.eofToken() = func(CompilationUnitTree::eofToken)
fun  ManyBuilder<out CompilationUnitTree>.eofToken() = func(CompilationUnitTree::eofToken)

fun  SingleBuilder<out CompilationUnitTree>.listImports() = func(CompilationUnitTree::imports)
fun  OptionalBuilder<out CompilationUnitTree>.listImports() = func(CompilationUnitTree::imports)
fun  ManyBuilder<out CompilationUnitTree>.listImports() = func(CompilationUnitTree::imports)
fun  SingleBuilder<out CompilationUnitTree>.imports() = listFunc(CompilationUnitTree::imports)
fun  OptionalBuilder<out CompilationUnitTree>.imports() = listFunc(CompilationUnitTree::imports)
fun  ManyBuilder<out CompilationUnitTree>.imports() = listFunc(CompilationUnitTree::imports)

fun  SingleBuilder<out CompilationUnitTree>.listTypes() = func(CompilationUnitTree::types)
fun  OptionalBuilder<out CompilationUnitTree>.listTypes() = func(CompilationUnitTree::types)
fun  ManyBuilder<out CompilationUnitTree>.listTypes() = func(CompilationUnitTree::types)
fun  SingleBuilder<out CompilationUnitTree>.types() = listFunc(CompilationUnitTree::types)
fun  OptionalBuilder<out CompilationUnitTree>.types() = listFunc(CompilationUnitTree::types)
fun  ManyBuilder<out CompilationUnitTree>.types() = listFunc(CompilationUnitTree::types)

fun  SingleBuilder<out CompilationUnitTree>.moduleDeclaration() = optFunc(CompilationUnitTree::moduleDeclaration)
fun  OptionalBuilder<out CompilationUnitTree>.moduleDeclaration() = optFunc(CompilationUnitTree::moduleDeclaration)
fun  ManyBuilder<out CompilationUnitTree>.moduleDeclaration() = optFunc(CompilationUnitTree::moduleDeclaration)

fun  SingleBuilder<out CompilationUnitTree>.packageDeclaration() = optFunc(CompilationUnitTree::packageDeclaration)
fun  OptionalBuilder<out CompilationUnitTree>.packageDeclaration() = optFunc(CompilationUnitTree::packageDeclaration)
fun  ManyBuilder<out CompilationUnitTree>.packageDeclaration() = optFunc(CompilationUnitTree::packageDeclaration)
