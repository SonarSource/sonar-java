package org.sonar.plugins.java.api.query.operation.generated

import org.sonarsource.astquery.operation.builder.SingleBuilder
import org.sonarsource.astquery.operation.builder.OptionalBuilder
import org.sonarsource.astquery.operation.builder.ManyBuilder
import org.sonar.plugins.java.api.tree.CaseGroupTree
import org.sonarsource.astquery.operation.composite.func
import org.sonarsource.astquery.operation.composite.listFunc

fun  SingleBuilder<out CaseGroupTree>.listBody() = func(CaseGroupTree::body)
fun  OptionalBuilder<out CaseGroupTree>.listBody() = func(CaseGroupTree::body)
fun  ManyBuilder<out CaseGroupTree>.listBody() = func(CaseGroupTree::body)
fun  SingleBuilder<out CaseGroupTree>.body() = listFunc(CaseGroupTree::body)
fun  OptionalBuilder<out CaseGroupTree>.body() = listFunc(CaseGroupTree::body)
fun  ManyBuilder<out CaseGroupTree>.body() = listFunc(CaseGroupTree::body)

fun  SingleBuilder<out CaseGroupTree>.listLabels() = func(CaseGroupTree::labels)
fun  OptionalBuilder<out CaseGroupTree>.listLabels() = func(CaseGroupTree::labels)
fun  ManyBuilder<out CaseGroupTree>.listLabels() = func(CaseGroupTree::labels)
fun  SingleBuilder<out CaseGroupTree>.labels() = listFunc(CaseGroupTree::labels)
fun  OptionalBuilder<out CaseGroupTree>.labels() = listFunc(CaseGroupTree::labels)
fun  ManyBuilder<out CaseGroupTree>.labels() = listFunc(CaseGroupTree::labels)
