from TokenType import TokenType
from AST_Nodes import NodeVisitor
from collections import OrderedDict
from SymbolTable import SymbolTable, VarSymbol, BuiltInTypeSymbol, FunctionSymbol

class SemanticAnalyzer(NodeVisitor):
    def __init__(self, parser):
        self.parser = parser
        self.current_scope = None

    def visit_Program(self, node):
        print("ENTER scope: global")
        global_scope = SymbolTable(
            scope_name="global",
            scope_level=1
        )
        self.current_scope = global_scope

        if(len(node.children) == 0):
            pass

        for child in node.children:
            self.visit(child)

        print(global_scope)
        print("LEAVE scope: global")

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
        # Initial scope is the scope you are coming from
        initial_scope = self.current_scope


        func_name = node.func_name
        func_symbol = FunctionSymbol(func_name)
        self.current_scope.insert(func_symbol)

        print("ENTER scope: %s" % func_name)
        function_scope = SymbolTable(
            scope_name=func_name,
            scope_level=2
        )
        self.current_scope = function_scope

        for parameter in node.parameters:
            self.visit(parameter)

        for child in node.children:
            self.visit(child)

        print(function_scope)
        print("LEAVE scope: %s" % func_name)
        self.current_scope = initial_scope

    def visit_VarDecl(self, node):
        type_name = node.type_node.value
        type_symbol = self.current_scope.lookup(type_name)

        var_name = node.var_node.value
        var_symbol = VarSymbol(var_name, type_symbol)

        if(self.current_scope.lookup(var_name) is not None):
            raise TypeError("Cannot initialize a variable that has already been declared '" + str(var_name) + "' on line: " + str(node.token.get_line()))

        self.current_scope.insert(var_symbol)

        if(node.assign_node):
            self.visit(node.assign_node)

    def visit_Variable(self, node):
        for var in node.var_decls:
            self.visit(var)

    def visit_Assign(self, node):
        var_name = node.left.value
        var_symbol = self.current_scope.lookup(var_name)
        if(var_symbol is None):
            raise NameError(repr(var_name) + " is an undefined variable on line: " + str(node.token.get_line()))

        self.visit(node.right)

    def visit_Identifier(self, node):
        var_name = node.value
        var_symbol = self.current_scope.lookup(var_name)

        if(var_symbol is None):
            raise NameError(repr(var_name) + " is an undefined variable on line: " + str(node.token.get_line()))


    def analyze(self):
        tree = self.parser.parse()
        self.visit(tree)
