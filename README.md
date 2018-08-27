# Dymond
This is an amazing language that is being built by yours truly! It has been a blast creating this and I hope some people will find use out of this.
Also, since I love helping others, if there are any additions that you would like to see, simply email me at ambrosiogabe@gmail.com.

## What is Dymond?
Dymond is an Object Oriented Programming language currently programmed in Java. In this repository there are three main folders, C-Source, Java, and Python.
Within each of these folder are different implementations of Dymond. The Python and Java version both use an AST, and are quite slow as a result. I plan
on building a Virtual Machine and compiler that compiles Dymond down to DByteCode. This will enhance the running speeds quite dramatically.

Coding in Dymond feels very similar to coding in Javascript, the only difference is that the underlying principles are closer to Python. What I mean by that
is that a lot of features in Python are mimicked in Dymond, but the syntax looks more like Javascript (Maybe I should call it PythonScript?).

Below I will give a slight overview of what you will find in each languages implementation of Dymond.

## Python
The Python implementation is the most inaccurate and sparse, so I would not recommend using it. Why keep it then? Well, this was my first stab
at how to go about building an **entire** language. So, I like to keep it there so that users can follow along the evolution of Dymond. It is fun to
experiment in, but the syntax is wildly different than what I actually settled on.

## Java
This was my next go at building a successful language. I changed a few things, for instance there is no longer declared types, they are inferred at runtime
now, also it has the whole Object Oriented part of OOP enabled! So, this is more reliable than the Python implementation and provides a solid overview
of what the final language will look like. Give the contents a look to see how it all fits together.

## To Run
Right now the programming language is still incomplete. Although it has quite a bit of functionality right now. If you would like to test it so far follow these instructions.

NOTE: You need Java installed on your computer in order for this to work.

1. Download the repository to a simple location on your PC, e.g C:\\Dymond or ~/Dymond
2. Edit the Programs/test_2.dy file to any program you would like to create.
3. Open a terminal or Cmd and change into the Java_Source/src directory.
4. Run javac dymond.Dymond
5. Change into the Programs/bin directory.
6. Run java dymond.Dymond

Here is a little sample of what the code looks like, with some comments explaining everything.

```
# This creates a Donut class
class Donut {
  # This is the initialization function
  func init() {
    print("I am a donut.");
  }
}

# This class extends Donut, or is a child of the Donut class
class GlazedDonut <- Donut {
  func init() {
    # This calls its parent's init function
    super.init();
    print("I am a glazed donut.");
  }
}

# I will add more and test later :)
```

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
