from Lexer import Lexer
from Token import Token
from Parser import Parser
from Interpreter import Interpreter
from SemanticAnalyzer import SemanticAnalyzer
from sys import argv
import os

def main():
    """program_name, file_name = argv
    working_directory = "/".join(os.path.abspath(program_name).split("\\")[:-1])
    print(working_directory)

    file_name = working_directory + "/" + file_name
    split_name = file_name.split("/")
    file_name = split_name[0]
    for i in split_name[1:]:
        print(file_name,i)
        file_name = os.path.join(file_name, i)"""

    #print(file_name)

    """
    in_file = open("input.py", "r")
    lexer = Lexer(in_file.read())
    in_file.close()
    tokens = lexer.tokenize()

    list = "["
    for i in range(len(tokens)):
        list += "'"

        if(tokens[i].get_value() != "\n"):
            list += tokens[i].get_value()
        else:
            list += "Newline"

        if(i == len(tokens) - 1):
            list += "']"
        else:
            list += "', "
    print(list)
    """

    lexer = Lexer(open("input.py", "r").read())
    se_parser = Parser(lexer, "input")
    semantic_analyzer = SemanticAnalyzer(se_parser)
    semantic_analyzer.analyze()

    lexer = Lexer(open("input.py", "r").read())
    in_parser = Parser(lexer, "input")
    semantic_analyzer.current_scope.reset_multi_scope_vars()
    interpreter = Interpreter(in_parser, semantic_analyzer.current_scope)
    result = interpreter.interpret()

if __name__ == "__main__":
    main()
