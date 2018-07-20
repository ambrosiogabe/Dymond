from TokenType import TokenType
from Token import Token
from SymbolTable import SymbolTable
from AST_Nodes import NodeVisitor

class Interpreter(NodeVisitor):
    def __init__(self, parser):
        self.parser = parser

        self.GLOBAL_SCOPE = {}

    # Custom interpretation functions for various methods
    def subtract_strings(self, left, right, token):
        returnStr = []
        if(right in left):
            returnStr = left.split(right)
            if(len(returnStr) == 2):
                for i in returnStr:
                    if(len(i) > 0):
                        returnStr = i
                        break

        if(type(returnStr).__name__ == "str"):
            return returnStr
        raise SyntaxError("Cannot subtract string '" + right + "' from string '" + left + "' on line: " + str(token.get_line()))

    def add_number_to_string(self, left, right, token, add=True):
        retStr = []
        for char in left:
            num = ord(char)
            if(add):
                num += right
            else:
                num -= right
            retStr.append(chr(num))
        return "".join(retStr)

    # This phase enables type checking to make sure your not doing anything illegal
    def visit_BinOp(self, node):
        left = self.visit(node.left)
        right = self.visit(node.right)

        if(node.token.get_type() == TokenType.PLUS):
            if(type(left).__name__ == "str" and type(right).__name__ == "int"):
                return self.add_number_to_string(left, right, node.token)
            elif(type(left).__name__ == "int" and type(right).__name__ == "str"):
                return self.add_number_to_string(right, left, node.token)
            if( type(left).__name__ != type(right).__name__):
                raise TypeError("Unsupported operand type '+' for types '" + type(left).__name__ + "' and '" + type(right).__name__ + "' on line: " + str(node.token.get_line()))
            return left + right
        elif(node.token.get_type() == TokenType.MINUS):
            if(type(left).__name__ == "str" and type(right).__name__ == "str"):
                return self.subtract_strings(left, right, node.token)
            elif(type(left).__name__ == "str" and type(right).__name__ == "int"):
                return self.add_number_to_string(left, right, node.token, False)
            if( type(left).__name__ != type(right).__name__):
                raise TypeError("Unsupported operand type '-' for types '" + type(left).__name__ + "' and '" + type(right).__name__ + "' on line: " + str(node.token.get_line()))
            return left - right
        elif(node.token.get_type() == TokenType.TIMES):
            if( type(left).__name__ != type(right).__name__):
                raise TypeError("Unsupported operand type '*' for types '" + type(left).__name__ + "' and '" + type(right).__name__ + "' on line: " + str(node.token.get_line()))
            return left * right
        elif(node.token.get_type() == TokenType.DIV):
            if(type(left).__name__ != type(right).__name__):
                raise TypeError("Unsupported operand type '/' for types '" + type(left).__name__ + "' and '" + type(right).__name__ + "' on line: " + str(node.token.get_line()))
            # If both types are integers, integer division
            # Floating point division must use at least one floating point number
            if(type(left).__name__ == "int" and type(right).__name__ == "int"):
                return left // right

            if(right == 0):
                raise ZeroDivisionError("Cannot divide by zero on line: " + str(node.token.get_line()))
            return left / right
        elif(node.token.get_type() == TokenType.INTEGER_DIV):
            if( type(left).__name__ != type(right).__name__):
                raise TypeError("Unsupported operand type '//' for types '" + type(left).__name__ + "' and '" + type(right).__name__ + "' on line: " + str(node.token.get_line()))

            if(right == 0):
                raise ZeroDivisionError("Cannot divide by zero on line: " + str(node.token.get_line()))
            return int(left // right)
        elif(node.token.get_type() == TokenType.MODULO):
            if( type(left).__name__ != type(right).__name__):
                raise TypeError("Unsupported operand type '%' for types '" + type(left).__name__ + "' and '" + type(right).__name__ + "' on line: " + str(node.token.get_line()))
            if(right == 0):
                raise ZeroDivisionError("Cannot divide by zero on line: " + str(node.token.get_line()))
            return left % right
        elif(node.token.get_type() == TokenType.CARET):
            if( type(left).__name__ != type(right).__name__):
                raise TypeError("Unsupported operand type '%' for types '" + type(left).__name__ + "' and '" + type(right).__name__ + "' on line: " + str(node.token.get_line()))
            return left ** right
        elif(node.token.get_type() == TokenType.AND):
            if(type(left).__name__ == "bool" and type(right).__name__ == "bool"):
                return (left and right)
            elif(type(left).__name__ == "bool" and type(right).__name__ != None):
                return (left and True)
            elif(type(left).__name__ != None and type(right).__name__ == "bool"):
                return (True and right)
            return False
        elif(node.token.get_type() == TokenType.OR):
            if(type(left).__name__ == "bool" and type(right).__name__ == "bool"):
                return (left or right)
            elif(type(left).__name__ == "bool" and type(right).__name__ != None):
                return (left or True)
            elif(type(left).__name__ != None and type(right).__name__ == "bool"):
                return (True or right)
            return False
        elif(node.token.get_type() == TokenType.DOUBLE_EQUAL):
            return left == right
        elif(node.token.get_type() == TokenType.NOT_EQUAL):
            return left == right
        elif(node.token.get_type() == TokenType.PLUS_PLUS):
            if(type(left).__name__ == "int" or type(left).__name__ == "float"):
                return left + 1
        elif(node.token.get_type() == TokenType.MINUS_MINUS):
            if(type(left).__name__ == "int" or type(left).__name__ == "float"):
                return left - 1

        raise SyntaxError("Unexpected syntax '" + node.token.get_type() + "' on line: " + str(node.token.get_line()))

    def visit_Integer(self, node):
        return int(node.token.get_value())

    def visit_Float(self, node):
        return float(node.token.get_value())

    def visit_String(self, node):
        return str(node.token.get_value())

    def visit_Bool(self, node):
        if(node.token.get_value() == "True"):
            return True
        elif(node.token.get_value() == "False"):
            return False

        raise SyntaxError("Expected true or false on line: " + str(node.token.get_line()))

    def visit_UnaryOperator(self, node):
        op = node.op.get_type()
        if(op == TokenType.MINUS):
            return -1 * self.visit(node.expr)
        elif(op == TokenType.PLUS):
            return self.visit(node.expr)
        elif(op == TokenType.NOT):
            return not self.visit(node.expr)
        elif(op == TokenType.PLUS_PLUS):
            return 1 + self.visit(node.expr)
        elif(op == TokenType.MINUS_MINUS):
            return self.visit(node.expr) - 1

    def visit_CompoundStatement(self, node):
        for child in node.children:
            self.visit(child)

    def visit_Program(self, node):
        if(len(node.children) == 0):
            self.visit_EmptyProgram()

        for child in node.children:
            self.visit(child)

    def visit_VarDecl(self, node):
        if(node.assign_node):
            self.visit(node.assign_node)
        pass

    def visit_Variable(self, node):
        for var_decl in node.var_decls:
            self.visit(var_decl)

    def visit_Type(self, node):
        pass

    def visit_Assign(self, node):
        var_name = node.left.token.get_value()
        self.GLOBAL_SCOPE[var_name] = self.visit(node.right)

    def visit_Identifier(self, node):
        var_name = node.token.get_value()
        val = self.GLOBAL_SCOPE[var_name]
        if(val is None):
            raise NameError("Variable: " + var_name + " is not defined on line: " + node.token.get_line())
        else:
            return val

    def visit_EmptyProgram(self, node):
        return

    def visit_FuncDecl(self, node):
        for child in node.children:
            self.visit(child)

    def visit_IfNode(self, node):
        validity = self.visit(node.validity)
        if(validity):
            for child in node.true_block:
                self.visit(child)
        else:
            if(node.false_block):
                for child in node.false_block:
                    self.visit(child)

    def interpret(self):
        tree = self.parser.parse()

        return self.visit(tree)
