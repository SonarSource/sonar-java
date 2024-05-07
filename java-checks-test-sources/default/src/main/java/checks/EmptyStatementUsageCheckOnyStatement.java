; // Noncompliant [[quickfixes=qf_first_statement]]
//^[sc=1;ec=1]

// fix@qf_first_statement {{Remove this empty statement}}
// edit@qf_first_statement [[sc=1;ec=2]] {{}}
