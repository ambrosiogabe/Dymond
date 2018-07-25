# Dymond
This is a programming language that was build in Python and has a Python-ish way of working. The program is parsed using a custom parser that I built. The parser breaks the language up into a bunch of tokens that it sends to my Interpreter. The interpreter then builds an abstract syntax tree and interprets what to do at each stage.

## To Run
Right now the programming language is still incomplete. Although it has quite a bit of functionality right now. If you would like to test it so far follow these instructions.

NOTE: You need Python 3 installed in order to be able to run this programming language. So install that first if you have not already.

1. Download the repository to a simple location on your PC, e.g C:\\Dymond or ~/Dymond
2. Once the repository is cloned, you need to add the place you installed it to your PATH environment variable.
3. In Windows you type in <code>env</code> on the search bar. Then open up Edit Environment Variables.
4. Click Enviornment Variables... Then search for PATH under System Variables.
5. Double click PATH and then click New. Type in the path where you installed dymond and then click OK.

To test if it works, you need to create a file and try it out. Create a file called <code>test.dy</code>. Open up the file and copy and paste the following code into it:

```
Int i = 0;
while(i < 10) {
  print(i);
  i++;
}
```

Then open up a Terminal or Command Prompt window in the same directory as the file you just created. Then type in <code>Dymond.py test.dy</code>. This should open a new Terminal or Command prompt and output something like this:

```
0
1
2
3
4
5
6
7
8
9
Press enter to exit...
```

Press enter to exit, and you have confirmed that Dymond now works on your computer! To run any other file simply create the file, and then run <code>Dymond.py file_name.dy</code> in the Terminal/Cmd and it will run you programs.

## How to Code in Dymond
Right now Dymond is pretty limited in its capabilities. I will list a sample program showcasing the majority of what Dymond is able to do so far.

```
Int my_int = 0;
Decimal my_decimal = 3e10;
Decimal my_other_decimal = 3.14;
Bool my_bool = True;
Bool my_other_bool = False;

for(Int i=0; i < 10; i++) {
  print(i);
}
print("\n\n");

if(my_int == 0) {
  print(toString(my_int) + "\n");
}

if(my_int == 10) {
  #do nothing because it doesn't
} else {
  print(my_int);
}

while(my_int < 10) {
  print(my_int);
  my_int++;
}
print("\n\n");

String user_in = input("Type some stuff in: ");
if(user_in == "Hi") {
  print("Hi back :)");
}

Int user_in_cast_to_int = toInt(input("Type an integer: "));
print(user_in_cast_to_int + 4);

Decimal dec = toDecimal(input("Type a decimal: "));
print(dec * 3.4);
```

As you can see the language currently supports for loops, while loops, if-else statements (it does not support if-else if-else yet...), casting, type checking, printing, and user input. I plan on adding more functionality as I get time. I also plan on creating an IDE so that you can experiment with simple statements.

## Parser
The parser goes through and looks for all the keywords and tokens that are defined in the TokenType class. It will turn a statement such as:

```
if (my_var == my_other_var) {
  break;
}
```

And then it breaks that whole statement into tokens. The broken up statement would look similar to this:

```
{'BOF', 'if', '(', 'my_var', '==', 'my_other_var', ')', '{', 'break', ';', ')', 'EOF'}
```

The BOF and EOF tokens are for to indicate the beginning of file, and end of file. The reason my program needs this is because it treats everything as an object. Therefore, the entire file that is in use is turned into one large object. This makes scoping things slightly simpler, because then I can keep everything scoped to its parent 'object', which in the case of the file is the whole program.

## Interpreter
The interpreter takes the list of tokens in the format indicated above and begins to build an abstract syntax tree out of them. All the pertinent classes are stored in the AST_Nodes and Interpreter classes. These basically create different classes for each type of branch on the abstract syntax tree. Then the interpreter takes each branch and decides what to do with it.

# Dymond Syntax
The syntax of the language dymond is...
