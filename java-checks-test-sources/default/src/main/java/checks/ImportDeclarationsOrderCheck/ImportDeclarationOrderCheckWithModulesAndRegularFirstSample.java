package checks.ImportDeclarationsOrderCheck;

// fix@qf1 {{Reorganize imports}}
// edit@qf1 [[sl=6;sc=1;el=10;ec=36]] {{import module java.base;\n\nimport java.sql.Date;\nimport java.util.List;\n\nimport static java.util.Collections.*;\nimport static java.lang.System.out;}}
// Module import not first, regular first
import java.sql.Date; // Noncompliant [[quickfixes=qf1]]
import java.util.List;
import module java.base;
import static java.util.Collections.*;
import static java.lang.System.out;

class ImportDeclarationOrderCheckWithModulesAndRegularFirstSample {
}
