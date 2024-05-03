package checks.OmitPermittedTypesCheck;

sealed class InSameFileA permits // Noncompliant {{Remove this redundant permitted list.}} [[quickfixes=qf0]]
//                       ^^^^^^^
  InSameFileB,
//  ^^^<
  InSameFileC,
//  ^^^<
  InSameFileD,
//  ^^^<
  InSameFileE {}
//  ^^^<
// fix@qf0 {{Remove permitted list}}
// edit@qf0 [[sl=3;sc=26;el=7;ec=15]] {{}}
final class InSameFileB extends InSameFileA {}
final class InSameFileC extends InSameFileA {}
final class InSameFileD extends InSameFileA {}
final class InSameFileE extends InSameFileA {}

sealed class InSameClassA {  // Compliant
  final class InSameClassB extends InSameClassA {}
  final class InSameClassC extends InSameClassA {}
}

sealed class InSameFile2A permits InSameFile2B, InSameFile2C {} // Noncompliant [[quickfixes=qf1]]
//                        ^^^^^^^
// fix@qf1 {{Remove permitted list}}
// edit@qf1 [[sc=27;ec=62]] {{}}
final class InSameFile2B extends InSameFile2A {}
sealed class InSameFile2C extends InSameFile2A permits InSameFile2D {} // Noncompliant {{Remove this redundant permitted list.}} [[quickfixes=qf2]]
//                                             ^^^^^^^
// fix@qf2 {{Remove permitted list}}
// edit@qf2 [[sc=48;ec=69]] {{}}
non-sealed class InSameFile2D extends InSameFile2C {}

sealed class InOtherFile2A permits InOtherFile2B, InOtherFile2C {} // Compliant

sealed class PartiallyInOtherFile2A permits PartiallyInOtherFile2B, PartiallyInOtherFile2C {} // Compliant, can not remove partially the list
final class PartiallyInOtherFile2B extends PartiallyInOtherFile2A {}

sealed class InSameFile3A {} // Compliant, list omitted
final class InSameFile3B extends InSameFile3A {}
