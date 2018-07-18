from TokenType import TokenType
from Token import Token
from AST_Nodes import NodeVisitor

class Interpreter(NodeVisitor):
    def __init__(self, parser):
        self.parser = parser
        self.GLOBAL_SCOPE = {}

    def print_global_scope(self):
        header = "Run-time GLOBAL_MEMORY contents:"
        lines = "_" * len(header)
        print(header + "\n" + lines)

        for k in self.GLOBAL_SCOPE:
            print(str(k) + " = " + str(self.GLOBAL_SCOPE[k]))

    def visit_BinOp(self, node):
        left = self.visit(node.left)
        right = self.visit(node.right)

        if(node.token.get_type() == TokenType.PLUS):
            return left + right
        elif(node.token.get_type() == TokenType.MINUS):
            return left - right
        elif(node.token.get_type() == TokenType.TIMES):
            return left * right
        elif(node.token.get_type() == TokenType.DIV):
            # If both types are integers, assume they want integer division
            # Floating point division must use at least one floating point number
            if(type(left).__name__ == "int" and type(right).__name__ == "int"):
                return left // right
            return left / right
        elif(node.token.get_type() == TokenType.INTEGER_DIV):
            return int(left // right)

    def visit_Integer(self, node):
        return int(node.token.get_value())

    def visit_Float(self, node):
        return float(node.token.get_value())

    def visit_UnaryOperator(self, node):
        op = node.op.get_type()
        if(op == TokenType.MINUS):
            return -1 * self.visit(node.expr)
        return self.visit(node.expr)

    def visit_CompoundStatement(self, node):
        for child in node.children:
            self.visit(child)

    def visit_Program(self, node):
        if(len(node.children) == 0):
            self.visit_EmptyProgram()

        for child in node.children:
            self.visit(child)

    def visit_VarDecl(self, node):
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

    def interpret(self):
        tree = self.parser.parse()

        return self.visit(tree)
