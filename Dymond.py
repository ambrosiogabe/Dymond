from Lexer import Lexer
from Token import Token
from Parser import Parser
from Interpreter import Interpreter
from SemanticAnalyzer import SemanticAnalyzer
from sys import argv
import os

def main():
    program_name, file_name = argv
    working_directory = os.getcwd()
    #print(working_directory)

    file_name = os.path.join(working_directory, file_name)

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
            list += str(tokens[i].get_value())
        else:
            list += "Newline"

        if(i == len(tokens) - 1):
            list += "']"
        else:
            list += "', "
    print(list)
    """

    lexer = Lexer(open(file_name, "r").read())
    se_parser = Parser(lexer, "input")
    semantic_analyzer = SemanticAnalyzer(se_parser)
    semantic_analyzer.analyze()

    lexer = Lexer(open(file_name, "r").read())
    in_parser = Parser(lexer, "input")
    semantic_analyzer.current_scope.reset_multi_scope_vars()
    interpreter = Interpreter(in_parser, semantic_analyzer.current_scope)
    result = interpreter.interpret()

if __name__ == "__main__":
    main()
    input("Press enter to exit...")
