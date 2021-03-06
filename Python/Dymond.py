from Lexer import Lexer
from Token import Token
from Parser import Parser
from Interpreter import Interpreter
from SemanticAnalyzer import SemanticAnalyzer
from sys import argv
import os
import traceback

def run_file(file_name):
    lexer = Lexer(open(file_name, "r").read())
    se_parser = Parser(lexer, "input")
    semantic_analyzer = SemanticAnalyzer(se_parser)
    semantic_analyzer.analyze()

    lexer = Lexer(open(file_name, "r").read())
    in_parser = Parser(lexer, "input")
    semantic_analyzer.current_scope.reset_multi_scope_vars()
    interpreter = Interpreter(in_parser, semantic_analyzer.current_scope)
    result = interpreter.interpret()


def run_ide():
    print(": Welcome to Dymond V0.0.0!")
    print(": Play around a little in this nice IDE type simple statements in and we will process them :)")
    print(": Type exit() to exit")
    user_in = ""
    total_user_in = ""

    while user_in != "exit()":
        user_in = input(">>> ")

        if(user_in == "exit()"):
            break

        try:
            test = total_user_in + user_in

            lexer = Lexer(test)
            se_parser = Parser(lexer, "ide")
            semantic_analyzer = SemanticAnalyzer(se_parser)

            semantic_analyzer.analyze()
            lexer = Lexer(test)
            in_parser = Parser(lexer, "ide")
            semantic_analyzer.current_scope.reset_multi_scope_vars()
            interpreter = Interpreter(in_parser, semantic_analyzer.current_scope)
            interpreter.interpret()

            if "print(" not in user_in and "input(" not in user_in:
                total_user_in += "\n" + user_in
        except Exception as ex:
            print(ex)


def main():
    program_name, file_name = argv
    working_directory = os.getcwd()

    if(file_name != "ide"):
        file_name = os.path.join(working_directory, file_name)
        run_file(file_name)
    else:
        run_ide()


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

if __name__ == "__main__":
    try:
        main()
    except Exception as ex:
        print(ex)
        #traceback.print_exc()
    finally:
        input("Press enter to exit...")
