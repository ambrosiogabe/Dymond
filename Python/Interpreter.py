from TokenType import TokenType
from Token import Token
from SymbolTable import SymbolTable
from AST_Nodes import NodeVisitor

class Interpreter(NodeVisitor):
    def __init__(self, parser, current_scope):
        self.parser = parser

        self.current_scope = current_scope
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

    # This phase enables type checking to make sure your not doing anything illegal
    def visit_BinOp(self, node):
        left = self.visit(node.left)
        right = self.visit(node.right)

        if(node.token.get_type() == TokenType.PLUS):
            if( type(left).__name__ != type(right).__name__):
                raise TypeError("Unsupported operand type '+' for types '" + type(left).__name__ + "' and '" + type(right).__name__ + "' on line: " + str(node.token.get_line()))
            return left + right
        elif(node.token.get_type() == TokenType.MINUS):
            if(type(left).__name__ == "str" and type(right).__name__ == "str"):
                return self.subtract_strings(left, right, node.token)
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
            return left != right
        elif(node.token.get_type() == TokenType.PLUS_PLUS):
            if(type(left).__name__ == "int" or type(left).__name__ == "float"):
                return left + 1
        elif(node.token.get_type() == TokenType.MINUS_MINUS):
            if(type(left).__name__ == "int" or type(left).__name__ == "float"):
                return left - 1
        elif(node.token.get_type() == TokenType.LESS):
            return left < right
        elif(node.token.get_type() == TokenType.GREATER):
            return left > right
        elif(node.token.get_type() == TokenType.LESS_OR_EQUAL):
            return left <= right
        elif(node.token.get_type() == TokenType.GREATER_OR_EQUAL):
            return left >= right

        raise SyntaxError("Unexpected syntax '" + node.token.get_type() + "' on line: " + str(node.token.get_line()))

    def visit_Integer(self, node):
        return int(node.token.value)

    def visit_Float(self, node):
        return float(node.token.value)

    def visit_String(self, node):
        return str(node.token.value)

    def visit_Bool(self, node):
        if(node.token.get_value() == "True"):
            return True
        elif(node.token.get_value() == "False"):
            return False

        raise SyntaxError("Expected true or false on line: " + str(node.token.get_line()))

    def visit_List(self, node):
        my_list = []
        for child in node.tokens:
            my_list.append(self.visit(child))

        return my_list

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
        # Program should only have one child: GLOBAL
        self.current_scope = self.current_scope.children["global"]

        if(len(node.children) == 0):
            self.visit_EmptyProgram()

        for child in node.children:
            self.visit(child)


        self.current_scope = self.current_scope.enclosing_scope

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
        var_symbol = self.current_scope.lookup(var_name)
        value = self.visit(node.right)
        type_of_symbol = str(var_symbol.type)
        type_of_value = type(value).__name__

        if(type_of_value == "str"):
            type_of_value = "String"
        elif(type_of_value == "int"):
            type_of_value = "Int"
        elif(type_of_value == "float"):
            type_of_value = "Decimal"
        elif(type_of_value == "list"):
            type_of_value = "List"
        elif(type_of_value == "bool"):
            type_of_value = "Bool"

        if(type_of_value != type_of_symbol):
            raise TypeError("Invalid type. Cannot assign '" + type_of_value + "' to a type of '" + str(type_of_symbol) + "' on line: " + str(node.token.get_line()))
        var_symbol.value = value

    def visit_Identifier(self, node):
        var_name = node.token.get_value()
        var_symbol = self.current_scope.lookup(var_name)
        val = var_symbol.value

        if(val is None):
            raise NameError("Variable: " + var_name + " is not defined on line: " + node.token.get_line())
        else:
            return val

    def visit_EmptyProgram(self):
        return

    def visit_FuncDecl(self, node):
        func_name = node.func_name
        self.current_scope = self.current_scope.children[func_name]

        for child in node.children:
            self.visit(child)


        self.current_scope = self.current_scope.enclosing_scope


    def visit_IfNode(self, node):
        self.current_scope.current_if += 1
        validity = self.visit(node.validity)
        if(validity):
            self.current_scope = self.current_scope.children["if" + str(self.current_scope.current_if)]
            for child in node.true_block:
                self.visit(child)


            self.current_scope = self.current_scope.enclosing_scope
        else:
            self.current_scope.current_else += 1
            if(node.false_block):
                self.current_scope = self.current_scope.children["else" + str(self.current_scope.current_else)]
                for child in node.false_block:
                    self.visit(child)


                self.current_scope = self.current_scope.enclosing_scope

    def visit_WhileNode(self, node):
        self.current_scope.current_while += 1
        while_scope = self.current_scope.children["while" + str(self.current_scope.current_while)]
        self.current_scope = while_scope

        condition = self.visit(node.condition)
        while(condition):
            self.current_scope.reset_multi_scope_vars()
            for child in node.true_block:
                self.visit(child)
            condition = self.visit(node.condition)

        self.current_scope = self.current_scope.enclosing_scope

    def visit_ForNode(self, node):
        self.current_scope.current_for += 1
        for_scope = self.current_scope.children["for" + str(self.current_scope.current_for)]
        self.current_scope = for_scope

        self.visit(node.variable)
        condition = self.visit(node.condition)
        while(condition):
            self.current_scope.reset_multi_scope_vars()
            self.visit(node.incrementer)
            condition = self.visit(node.condition)
            if(condition):
                for child in node.for_block:
                    self.visit(child)

        self.current_scope = self.current_scope.enclosing_scope



    def visit_FunctionCall(self, node):
        previous_scope = self.current_scope

        parameters = []

        for parameter in node.parameters:
            parameters.append(self.visit(parameter))

        self.current_scope = self.current_scope.return_scope_of(node.func_name.value)


        self.current_scope = previous_scope

    """---------------------
    # Native function calls
    ---------------------"""
    def visit_Print(self, node):
        params = node.params
        if(len(params) == 0):
            print()
        elif(len(params) == 1):
            print(self.visit(params[0]))
        elif(len(params) == 2):
            print(self.visit(params[0]), end=self.visit((params[1])))

    def visit_ToString(self, node):
        param = self.visit(node.param)
        return str(param)

    def visit_ToInt(self, node):
        param = self.visit(node.param)
        return int(param)

    def visit_ToDecimal(self, node):
        param = self.visit(node.param)
        return float(param)

    def visit_Input(self, node):
        param = self.visit(node.param)
        if(param is None):
            return input()

        return input(param)

    def interpret(self):
        tree = self.parser.parse()

        return self.visit(tree)
