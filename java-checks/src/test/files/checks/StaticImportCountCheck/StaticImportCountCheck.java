import static java.lang.Math.*; // Noncompliant [[sc=1;ec=32;secondary=1,2,3,4,5,6,7]] {{Reduce the number of "static" imports in this class from 7 to the maximum allowed 4.}}
import static java.util.Collections.*;
import static com.myco.corporate.Constants.*;
import static com.myco.division.Constants.*;
import static com.myco.department.Constants.*;
import static com.myco.department.Constants.*;
import static com.myco.department.Constants.*;
