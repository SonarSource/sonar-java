package checks;

public class DepthOfInheritanceTreeCheckIntermediateMatching {}
class Dit_A {}
class Dit_B extends Dit_A {}
class Dit_C extends Dit_B {} // Noncompliant {{This class has 3 parents which is greater than 2 authorized.}}
class Dit_D extends Dit_C {}
class Dit_E extends Dit_D {}
class Dit_F extends Dit_E {}
class Dit_G extends Dit_F {} // Noncompliant {{This class has 3 parents which is greater than 2 authorized.}}
