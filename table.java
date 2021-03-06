
/*******************************************************************************
* @file Table.java
*
* @author John Miller
*/

import java.io.Serializable;
import static java.lang.Boolean.*;
import static java.lang.System.out;
import java.util.*;

/*******************************************************************************
* This class implements relational database tables (including attribute names,
* domains and a list of tuples. Five basic relational algebra operators are
* provided: project, select, union, minus and join. The insert data manipulation
* operator is also provided. Missing are update and delete data manipulation
* operators.
*/
public class Table
       implements Serializable, Cloneable
{
    /** Debug flag, turn off once implemented
*/
    private static final boolean DEBUG = true;

    /** Counter for naming temporary tables.
*/
    private static int count = 0;

    /** Table name.
*/
    private final String name;

    /** Array of attribute names.
*/
    private final String [] attribute;

    /** Array of attribute domains: a domain may be
* integer types: Long, Integer, Short, Byte
* real types: Double, Float
* string types: Character, String
*/
    private final Class [] domain;

    /** Collection of tuples (data storage).
*/
    private final List <Comparable []> tuples;

    /** Primary key.
*/
    private final String [] key;

    /** Index into tuples (maps key to tuple).
*/
    private final Map <KeyType, Comparable []> index;

    /***************************************************************************
* Construct an empty table from the meta-data specifications.
* @param _name the name of the relation
* @param _attribute the string containing attributes names
* @param _domain the string containing attribute domains (data types)
* @param _key the primary key
*/
    public Table (String _name, String [] _attribute, Class [] _domain, String [] _key)
    {
        name = _name;
        attribute = _attribute;
        domain = _domain;
        key = _key;
        tuples = new ArrayList <Comparable[]> (); // also try FileList, see below
// tuples = new FileList (this, tupleSize ());
        index = new TreeMap <KeyType, Comparable[]> (); // also try BPTreeMap, LinHash or ExtHash
    } // Table

    /***************************************************************************
* Construct an empty table from the raw string specifications.
* @param name the name of the relation
* @param attributes the string containing attributes names
* @param domains the string containing attribute domains (data types)
*/
    public Table (String name, String attributes, String domains, String _key)
    {
        this (name, attributes.split (" "), findClass (domains.split (" ")), _key.split(" "));

        out.println ("DDL> create table " + name + " (" + attributes + ")");
    } // Table

    /***************************************************************************
* Construct an empty table using the meta-data of an existing table.
* @param tab the table supplying the meta-data
* @param suffix the suffix appended to create new table name
*/
    public Table (Table tab, String suffix)
    {
        this (tab.name + suffix, tab.attribute, tab.domain, tab.key);
    } // Table

    /***************************************************************************
* Project the tuples onto a lower dimension by keeping only the given attributes.
* Check whether the original key is included in the projection.
* #usage movie.project ("title year studioNo")
* @param attributeList the attributes to project onto
* @return the table consisting of projected tuples
*/
    
    public Table project (String attributeList)
    {
        out.println ("RA> " + name + ".project (" + attributeList + ")");

        String [] pAttribute = attributeList.split (" ");
        int [] colPos = match (pAttribute);
        Class [] colDomain = extractDom (domain, colPos);
        String [] newKey = null; // FIX: original key if included, otherwise all atributes
        Table result = new Table (name + count++, pAttribute, colDomain, newKey);

        for (Comparable [] tup : tuples) {
            result.tuples.add (extractTup (tup, colPos));
        } // for

        return result;
    } // project

    /***************************************************************************
* Select the tuples satisfying the given condition.
* A condition is written as infix expression consists of
* 6 comparison operators: "==", "!=", "<", "<=", ">", ">="
* 2 Boolean operators: "&", "|" (from high to low precedence)
* #usage movie.select ("1979 < year & year < 1990")
* @param condition the check condition for tuples
* @return the table consisting of tuples satisfying the condition
*/
    public Table select (String condition)
    {
        out.println ("RA> " + name + ".select (" + condition + ")");

        //String [] postfix = { "title", "Star_Wars", "==" }; // FIX: delete after impl
        String [] postfix = infix2postfix (condition); // FIX: uncomment after impl
        Table result = new Table (name + count++, attribute, domain, key);

        for (Comparable [] tup : tuples) {
            if (evalTup (postfix, tup)) result.tuples.add (tup);
        } // for

        return result;
    } // select

    /***************************************************************************
* Union this table and table2. Check that the two tables are compatible.
* #usage movie.union (show)
* @param table2 the rhs table in the union operation
* @return the table representing the union (this U table2)
* @author: minh pham
*/
    public Table union (Table table2)
    {
        out.println ("RA> " + name + ".union (" + table2.name + ")");

        Table result = new Table (name + count++, attribute, domain, key);

        if (!this.compatible(table2)){
            System.out.println("tables are not compatible");
          return result;
        }
        int length1 = this.tuples.size();
        for(int i=0; i< length1; i++){
        	result.tuples.add(this.tuples.get(i));
        }
        int length2 = table2.tuples.size();
        for (int i=0; i< length2; i++){
        	int j=0;
        	while(j<length1 && !isEqual(this.tuples.get(j), table2.tuples.get(i)) ){
        		j++;
        	}
        	if (j==length1){
        		result.tuples.add(table2.tuples.get(i));
        	}
        }
        
        
        return result;
    } // union

    
    /***************************************************************************
* Take the difference of this table and table2. Check that the two tables
* are compatible.
* #usage movie.minus (show)
* @param table2 the rhs table in the minus operation
* @return the table representing the difference (this - table2)
* @author minh pham
*/
    public Table minus (Table table2)
    {
        out.println ("RA> " + name + ".minus (" + table2.name + ")");

        Table result = new Table (name + count++, attribute, domain, key);

        if (!this.compatible(table2)){
            System.out.println("tables are not compatible");
        	return result;
        }
        
        for(int i=0; i<this.tuples.size(); i++){
        	int j=0;
        	while(j<table2.tuples.size() && !isEqual(this.tuples.get(i),table2.tuples.get(j))){
        		j++;
        	}
        	if(j==table2.tuples.size()){
        		result.tuples.add(this.tuples.get(i));
        	}
        	
        }
        
        return result;
    } // minus

    /***************************************************************************
* Join this table and table2. If an attribute name appears in both tables,
* assume it is from the first table unless it is qualified with the first
* letter of the second table's name (e.g., "s.").
* In the result, disambiguate the attribute names in a similar way
* (e.g., prefix the second occurrence with "s_").
* Caveat: the key parameter assumes joining the table with the foreign key
* (this) to the table containing the primary key (table2).
* #usage movie.join ("studioNo == name", studio);
* #usage movieStar.join ("name == s.name", starsIn);
* @param condition the join condition for tuples
* @param table2 the rhs table in the join operation
* @return the table representing the join (this |><| table2)
*/
    public Table join (String condition, Table table2)
    {
        out.println ("RA> " + name + ".join (" + condition + ", " + table2.name + ")");

        Table result = new Table (name + count++, new String [0], new Class [0], key);

             //-----------------\\
            // TO BE IMPLEMENTED \\
           //---------------------\\

        return result;
    } // join

    /***************************************************************************
* Insert a tuple to the table.
* #usage movie.insert ("'Star_Wars'", 1977, 124, "T", "Fox", 12345)
* @param tup the array of attribute values forming the tuple
* @return whether insertion was successful
*/
    public boolean insert (Comparable [] tup)
    {
        out.println ("DML> insert into " + name + " values ( " + Arrays.toString (tup) + " )");

        if (typeCheck (tup, domain)) {
            tuples.add (tup);
            Comparable [] keyVal = new Comparable [key.length];
            int [] cols = match (key);
            for (int j = 0; j < keyVal.length; j++) keyVal [j] = tup [cols [j]];
            index.put (new KeyType (keyVal), tup);
            return true;
        } else {
            return false;
        } // if
    } // insert

    /***************************************************************************
* Get the name of the table.
* @return the table's name
*/
    public String getName ()
    {
        return name;
    } // getName
    
    /***************************************************************************
* Get the attribute at the indexed column of the table
* @param index the index number of the array item you want to access
* @return the attribute at the given index
* @author Nicholas Sobrilsky
*/
    private String getAttributeAt(int index)
    {
        return attribute[index];
    } //getAttributeAt
    
     /***************************************************************************
* Get the domain (class) at the indexed column of the table
* @param index the index number of the array item you want to access
* @return the domain (class) at the given index
* @author Nicholas Sobrilsky
*/
    private Class getDomainAt(int index){
return domain[index];
    } //getDomainAt
    
     /***************************************************************************
* Get the length of the attribute array of the table
* @return the length of the attribute array
* @author Nicholas Sobrilsky
*/
    private int getAttributeLength(){
return attribute.length;
    } //getAttributeArray
    
     /***************************************************************************
* Get the length of the domain array of the table
* @return the length of the domain array
* @author Nicholas Sobrilsky
*/
    private int getDomainLength(){
return domain.length;
    } //getDomainLength

    /***************************************************************************
* Print the table.
*/
    public void print ()
    {
        out.println ("\n Table " + name);

        out.print ("|-");
        for (int i = 0; i < attribute.length; i++) out.print ("---------------");
        out.println ("-|");
        out.print ("| ");
        for (String a : attribute) out.printf ("%15s", a);
        out.println (" |");

        if (DEBUG) {
            out.print ("|-");
            for (int i = 0; i < domain.length; i++) out.print ("---------------");
            out.println ("-|");
            out.print ("| ");
            for (Class d : domain) out.printf ("%15s", d.getSimpleName ());
            out.println (" |");
        } // if

        out.print ("|-");
        for (int i = 0; i < attribute.length; i++) out.print ("---------------");
        out.println ("-|");
        for (Comparable [] tup : tuples) {
            out.print ("| ");
            for (Comparable attr : tup) out.printf ("%15s", attr);
            out.println (" |");
        } // for
        out.print ("|-");
        for (int i = 0; i < attribute.length; i++) out.print ("---------------");
        out.println ("-|");
    } // print

    /***************************************************************************
* Determine whether the two tables (this and table2) are compatible, i.e.,
* have the same number of attributes each with the same corresponding domain.
* @param table2 the rhs table
* @return whether the two tables are compatible
* @author Nicholas Sobrilsky
*/
    private boolean compatible (Table table2)
    {
        if ( this.getAttributeLength()!=table2.getAttributeLength() ){
              return false;
        }

        int i=0;
        while(i<this.getDomainLength() && this.getAttributeAt(i).compareToIgnoreCase(table2.getAttributeAt(i))==0 && this.getDomainAt(i).getName().compareToIgnoreCase(table2.getDomainAt(i).getName())==0){
        	i++;
        }
        if(i==this.getDomainLength()){
        	return true;
        }
        return false;
        
    } // compatible

    /***************************************************************************
* Return the column position for the given column/attribute name.
* @param column the given column/attribute name
* @return the column index position
*/
    private int columnPos (String column)
    {
        for (int j = 0; j < attribute.length; j++) {
           if (column.equals (attribute [j])) return j;
        } // for

        out.println ("columnPos: error - " + column + " not found");
        return -1; // column name not found in this table
    } // columnPos

    /***************************************************************************
* Return all the column positions for the given column/attribute names.
* @param columns the array of column/attribute names
* @return the array of column index positions
*/
    private int [] match (String [] columns)
    {
        int [] colPos = new int [columns.length];

        for (int i = 0; i < columns.length; i++) {
            colPos [i] = columnPos (columns [i]);
        } // for

        return colPos;
    } // match
    

    /***************************************************************************
* Check whether the tuple satisfies the condition. Use a stack-based postfix
* expression evaluation algorithm.
* @param postfix the postfix expression for the condition
* @param tup the tuple to check
* @return whether to keep the tuple
*/
    @SuppressWarnings("unchecked")
    private boolean evalTup (String [] postfix, Comparable [] tup)
    {
        if (postfix == null) return true;
        Stack <Comparable> s = new Stack <Comparable> ();
        
        for (String token : postfix) {
        	if(precedenceInt(token)==0){
        		s.add(token);
        	}
        	else if(precedenceInt(token)==3){
        		String opt = new String(token);
        		String value = (String) s.pop();
        		String pattribute = (String) s.pop();
        		int colPos = columnPos(pattribute);
        		Class dom = tup[colPos].getClass();
        		if(compare(tup[colPos].toString(), opt, value)){
        			s.add(true);
        		}
        		else{
        			s.add(false);
        		}
        	}
        	else{
        		String opt = new String(token);
        		boolean bool2 = (Boolean) s.pop();
        		boolean bool1 = (Boolean) s.pop();
        		if(opt.equals("&")){
        			s.add(bool1 && bool2);
        		}
        		else{
        			s.add(bool1 || bool2);
        		}
        	}


        } // for
    	System.out.println();

        return (Boolean) s.pop ();
    } // evalTup

    /***************************************************************************
* Pack tuple tup into a record/byte-buffer (array of bytes).
* @param tup the array of attribute values forming the tuple
* @return a tuple packed into a record/byte-buffer
*
byte [] pack (Comparable [] tup)
{
byte [] record = new byte [tupleSize ()];
byte [] b = null;
int s = 0;
int i = 0;

for (int j = 0; j < domain.length; j++) {
switch (domain [j].getName ()) {
case "java.lang.Integer":
b = Conversions.int2ByteArray ((Integer) tup [j]);
s = 4;
break;
case "java.lang.String":
b = ((String) tup [j]).getBytes ();
s = 64;
break;

//-----------------\\
// TO BE IMPLEMENTED \\
//---------------------\\

} // switch
if (b == null) {
out.println ("Table.pack: byte array b is null");
return null;
} // if
for (int k = 0; k < s; k++) record [i++] = b [k];
} // for
return record;
} // pack
*/

    /***************************************************************************
* Unpack the record/byte-buffer (array of bytes) to reconstruct a tuple.
* @param record the byte-buffer in which the tuple is packed
* @return an unpacked tuple
*
Comparable [] unpack (byte [] record)
{
//-----------------\\
// TO BE IMPLEMENTED \\
//---------------------\\

return null;
} // unpack
*/

    /***************************************************************************
* Determine the size of tuples in this table in terms of the number of bytes
* required to store it in a record/byte-buffer.
* @return the size of packed-tuples in bytes
*
private int tupleSize ()
{
int s = 0;

for (int j = 0; j < domain.length; j++) {
switch (domain [j].getName ()) {
case "java.lang.Integer": s += 4; break;
case "java.lang.String": s += 64; break;

//-----------------\\
// TO BE IMPLEMENTED \\
//---------------------\\

} // if
} // for

return s;
} // tupleSize
*/

    //------------------------ Static Utility Methods --------------------------

    /***************************************************************************
* Check the size of the tuple (number of elements in list) as well as the
* type of each value to ensure it is from the right domain.
* @param tup the tuple as a list of attribute values
* @param dom the domains (attribute types)
* @return whether the tuple has the right size and values that comply
* with the given domains
* @author: minh pham
*/
    private static boolean typeCheck (Comparable [] tup, Class [] dom)
    {

    	if(tup.length!=dom.length){
    		return false;
    	}
    	int length = tup.length;
    	for(int i=0; i< length; i++){
    		if(tup[i].getClass().getName()!=dom[i].getName()){
    			return false;
    		}
    	}
    	
        return true;
    } // typeCheck

    /***************************************************************************
* Determine if the token/op is a comparison operator.
* @param op the token/op to check
* @return whether it a comparison operator
*/
    private static boolean isComparison (String op)
    {
        return op.equals ("==") || op.equals ("!=") ||
               op.equals ("<") || op.equals ("<=") ||
               op.equals (">") || op.equals (">=");
    } // isComparison

    /***************************************************************************
* Compare values x and y according to the comparison operator.
* @param x the first operand
* @param op the comparison operator
* @param y the second operand
* @return whether the comparison evaluates to true or false
*/
    @SuppressWarnings("unchecked")
    private static boolean compare (Comparable x, String op , Comparable y)
    {
        
        if(op.equals("==")) return x.compareTo (y) == 0;
        else if(op.equals("!="))  return x.compareTo (y) != 0;
        else if(op.equals("<")) return x.compareTo (y) < 0;
        else if(op.equals("<=")) return x.compareTo (y) <= 0;
        else if(op.equals(">"))return x.compareTo (y) > 0;
        else if(op.equals(">="))return x.compareTo (y) >= 0;
        else { out.println ("compare: error - unexpected op"); return false; }

    } // compare

    /***************************************************************************
* Convert an untokenized infix expression to a tokenized postfix expression.
* This implementation does not handle parentheses ( ).
* Ex: "1979 < year & year < 1990" --> { "1979", "year", "<", "year", "1990", "<", "&" }
* @param condition the untokenized infix condition
* @return resultant tokenized postfix expression
*/
    
    public static String [] infix2postfix (String condition)
    {
    	if (condition == null || condition.trim () == "") return null;
        String [] infix = condition.split (" "); // tokenize the infix
        String [] postfix = new String [infix.length]; // same size, since no ( )

        Stack ops = new Stack();
        int postPos=0;
        for(int i =0; i<infix.length; i++){
        	if(precedenceInt(infix[i])==0){
        		postfix[postPos] = new String(infix[i]);
        		postPos++;
        	}
        	else if(ops.empty()){
        		ops.add(infix[i]);
        	}
        	else{
        		if(precedenceInt(infix[i])>precedenceInt((String) ops.peek())){
        			ops.add(infix[i]);
        		}
        		else{

        			while(!ops.empty() && precedenceInt((String) ops.peek()) >= precedenceInt(infix[i])){
        				postfix[postPos] = new String((String) ops.pop());
        				postPos++;
        			}
        			ops.add(infix[i]);
        		}
        	}
        }
        while(!ops.empty()){
        	postfix[postPos] = new String((String) ops.pop());
        	postPos++;
        }
        return postfix;
    
    
/**
* A function to compare the two precedence of two operators
* #usage precedence("==","!=")
* @author Ryan Gell
* @return true if first has higher precedence thant second
*/

    private static boolean precedence(String first, String second){
if(precedenceInt(first) > precedenceInt(second)){
return true;
}
else{return false;}
}

/**
* Returns an int equal to the precedence of the operator
* #usage precedenceInt("==")
* @author Ryan Gell
* @return int between 8 and 0
*/
    private static int precedenceInt(String x){
    	int temp;

    	if(x.compareTo("==")==0 || x.compareTo("!=")==0 || x.compareTo("<")==0 || x.compareTo("<=")==0 || x.compareTo(">")==0 || x.compareTo(">=")==0) temp = 3;
    	else if(x.compareTo("&")==0) temp = 2;
    	else if(x.compareTo("|")==0) temp = 1; 
    	else temp = 0;
    	return temp;
    }
    /***************************************************************************
* Find the classes in the "java.lang" package with given names.
* @param className the array of class name (e.g., {"Integer", "String"})
* @return the array of Java classes for the corresponding names
*/
    private static Class [] findClass (String [] className)
    {
        Class [] classArray = new Class [className.length];

        for (int i = 0; i < className.length; i++) {
            try {
                classArray [i] = Class.forName ("java.lang." + className [i]);
            } catch (ClassNotFoundException ex) {
                out.println ("findClass: " + ex);
            } // try
        } // for

        return classArray;
    } // findClass

    /***************************************************************************
* Extract the corresponding domains from the group.
* @param group where to extract from
* @param colPos the column positions to extract
* @return the extracted domains
*/
    private static Class [] extractDom (Class [] group, int [] colPos)
    {
        Class [] dom = new Class [colPos.length];

        for (int j = 0; j < colPos.length; j++) {
            dom [j] = group [colPos [j]];
        } // for

        return dom;
    } // extractDom

    /***************************************************************************
* Extract the corresponding attribute values from the group.
* @param group where to extract from
* @param colPos the column positions to extract
* @return the extracted attribute values
*/
    private static Comparable [] extractTup (Comparable [] group, int [] colPos)
    {
        Comparable [] tup = new Comparable [colPos.length];

        for (int i=0; i<tup.length; i++){
        	tup[i]=group[colPos[i]];
        }

        return tup;
    } // extractTup

    private static boolean isEqual(Comparable[] table1, Comparable[] table2){
    	int size = table1.length;
    	int i=0;
    	while(i<size && table1[i].compareTo(table2[i])==0){
    		i++;
    	}
    	if(i==size){
    		return true;
    	}
    	return false;
    }
    
} // Table class

