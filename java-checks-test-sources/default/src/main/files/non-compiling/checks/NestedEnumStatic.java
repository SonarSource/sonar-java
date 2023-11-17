package checks;

// should be rejected during compilation
static enum MyStaticEnum {// Noncompliant [[sc=1;ec=7;secondary=4]] {{Remove this redundant "static" qualifier; nested enum types are implicitly static.}}
}
