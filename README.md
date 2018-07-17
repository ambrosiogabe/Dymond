# Dymond
This is a programming language that was build in Python and has a Python-ish way of working. The program is parsed using a custom parser that I built. The parser breaks the language up into a bunch of tokens that it sends to my Interpreter. The interpreter then builds an abstract syntax tree and interprets what to do at each stage.

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
