package checks.UselessImportCheck;

import java.util.List; // Noncompliant [[quickfixes=qf1]]
//     ^^^^^^^^^^^^^^
//fix@qf1 {{Remove the import}}
//edit@qf1 [[sl=3;sc=1;el=3;ec=23]] ̣{{}}
class WithQuicFixesSingleImport {
}
