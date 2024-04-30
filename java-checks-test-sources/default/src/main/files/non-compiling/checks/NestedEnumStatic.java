package checks;

// should be rejected during compilation
static enum MyStaticEnum { // Noncompliant {{Remove this redundant "static" qualifier; nested enum types are implicitly static.}}
//^[sc=1;ec=6]
//     ^^^^@-1<
}
