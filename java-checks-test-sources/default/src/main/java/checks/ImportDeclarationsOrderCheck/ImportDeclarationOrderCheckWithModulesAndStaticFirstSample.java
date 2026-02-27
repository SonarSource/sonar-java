package checks.ImportDeclarationsOrderCheck;

// fix@qf1 {{Reorganize imports}}
// edit@qf1 [[sl=6;sc=1;el=10;ec=23]] {{import module java.base;\n\nimport static java.util.Collections.*;\nimport static java.lang.System.out;\n\nimport java.sql.Date;\nimport java.util.List;}}
// Module import not first, static first
import static java.util.Collections.*; // Noncompliant [[quickfixes=qf1]]
import static java.lang.System.out;
import module java.base;
import java.sql.Date;
import java.util.List;

class ImportDeclarationOrderCheckWithModulesAndStaticFirstSample {
}
