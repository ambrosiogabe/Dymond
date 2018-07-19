from TokenType import TokenType
from AST_Nodes import NodeVisitor
from collections import OrderedDict
from SymbolTable import SymbolTable, VarSymbol, BuiltInTypeSymbol

class SemanticAnalyzer(NodeVisitor):
    def __init__(self, parser):
        self.parser = parser
        self.symtab = SymbolTable()

    def visit_Program(self, node):
        if(len(node.children) == 0):
            pass

        for child in node.children:
            self.visit(child)

    def visit_BinOp(self, node):
        self.visit(node.left)
        self.visit(node.right)

    def visit_Integer(self, node):
        pass

    def visit_Float(self, node):
        pass

    def visit_UnaryOp(self, node):
        self.visit(node.expr)

    def visit_CompoundStatement(self, node):
        for child in node.children:
            self.visit(child)

    def visit_EmptyProgram(self, node):
        pass

    def visit_FuncDecl(self, node):
        for parameter in node.parameters:
            self.visit(parameter)

        for child in node.children:
            self.visit(child)

    def visit_VarDecl(self, node):
        type_name = node.type_node.value
        type_symbol = self.symtab.lookup(type_name)

        var_name = node.var_node.value
        var_symbol = VarSymbol(var_name, type_symbol)

        if(self.symtab.lookup(var_name) is not None):
            raise TypeError("Cannot initialize a variable that has already been declared '" + str(var_name) + "' on line: " + str(node.token.get_line()))

        self.symtab.insert(var_symbol)

        if(node.assign_node):
            self.visit(node.assign_node)

    def visit_Variable(self, node):
        for var in node.var_decls:
            self.visit(var)

    def visit_Assign(self, node):
        var_name = node.left.value
        var_symbol = self.symtab.lookup(var_name)
        if(var_symbol is None):
            raise NameError(repr(var_name) + " is an undefined variable on line: " + str(node.token.get_line()))

        self.visit(node.right)

    def visit_Identifier(self, node):
        var_name = node.value
        var_symbol = self.symtab.lookup(var_name)

        if(var_symbol is None):
            raise NameError(repr(var_name) + " is an undefined variable on line: " + str(node.token.get_line()))


    def analyze(self):
        tree = self.parser.parse()
        self.visit(tree)
