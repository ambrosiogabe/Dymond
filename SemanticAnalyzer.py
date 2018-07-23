from TokenType import TokenType
from Token import Token
from AST_Nodes import NodeVisitor
from collections import OrderedDict
from SymbolTable import SymbolTable, VarSymbol, BuiltInTypeSymbol, FunctionSymbol

class SemanticAnalyzer(NodeVisitor):
    def __init__(self, parser):
        self.parser = parser
        self.current_scope = None
        self.program_name = self.parser.program_name

    def visit_Program(self, node):
        print("ENTER scope: " + self.program_name)
        builtin_scope = SymbolTable(
            scope_name=self.program_name,
            scope_level=0,
            enclosing_scope=None
        )
        self.current_scope = builtin_scope

        print("ENTER scope: global")
        global_scope = SymbolTable(
            scope_name="global",
            scope_level=1,
            enclosing_scope=builtin_scope # None
        )
        self.current_scope.children[global_scope.scope_name] = global_scope
        self.current_scope = global_scope

        if(len(node.children) == 0):
            pass

        for child in node.children:
            self.visit(child)

        print(global_scope)
        self.current_scope = self.current_scope.enclosing_scope
        print("LEAVE scope: global")

        print(builtin_scope)
        print("LEVAE scope: " + self.program_name)

    def visit_BinOp(self, node):
        left_symbol = self.visit(node.left)
        right_symbol = self.visit(node.right)


    def visit_Integer(self, node):
        pass

    def visit_Float(self, node):
        pass

    def visit_String(self, node):
        pass

    def visit_Bool(self, node):
        pass

    def visit_UnaryOperator(self, node):
        self.visit(node.expr)

    def visit_CompoundStatement(self, node):
        for child in node.children:
            self.visit(child)

    def visit_EmptyProgram(self, node):
        pass

    def visit_FuncDecl(self, node):
        func_name = node.func_name
        func_symbol = FunctionSymbol(func_name)
        self.current_scope.insert(func_symbol)

        print("ENTER scope: %s" % func_name)
        function_scope = SymbolTable(
            scope_name=func_name,
            scope_level=self.current_scope.scope_level + 1,
            enclosing_scope=self.current_scope
        )
        self.current_scope.children[func_name] = function_scope
        self.current_scope = function_scope

        for parameter in node.parameters:
            self.visit(parameter)

        for child in node.children:
            self.visit(child)

        print(function_scope)
        print("LEAVE scope: %s" % func_name)
        self.current_scope = self.current_scope.enclosing_scope

    def visit_IfNode(self, node):
        self.current_scope.current_if += 1
        print("ENTER scope: if" + str(self.current_scope.current_if))
        if_scope = SymbolTable(
            scope_name="if" + str(self.current_scope.current_if),
            scope_level=self.current_scope.scope_level + 1,
            enclosing_scope=self.current_scope
        )
        self.current_scope.children[if_scope.scope_name] = if_scope
        self.current_scope = if_scope
        for child in node.true_block:
            self.visit(child)

        print(if_scope)
        print("LEAVE scope: if")
        self.current_scope = self.current_scope.enclosing_scope

        if(node.false_block):
            self.current_scope.current_else += 1
            print("ENTER scope: else" + str(self.current_scope.current_else))
            else_scope = SymbolTable(
                scope_name="else" + str(self.current_scope.current_else),
                scope_level=self.current_scope.scope_level + 1,
                enclosing_scope=self.current_scope
            )
            self.current_scope.children[else_scope.scope_name] = else_scope
            self.current_scope = else_scope

            for child in node.false_block:
                self.visit(child)
            print(else_scope)
            print("LEAVE scope: else")
            self.current_scope = self.current_scope.enclosing_scope

    def visit_WhileNode(self, node):
        self.current_scope.current_while += 1
        print("ENTER scope: while" + str(self.current_scope.current_while))
        while_scope = SymbolTable(
            scope_name="while" + str(self.current_scope.current_while),
            scope_level=self.current_scope.scope_level + 1,
            enclosing_scope=self.current_scope
        )
        self.current_scope.children[while_scope.scope_name] = while_scope
        self.current_scope = while_scope
        for child in node.true_block:
            self.visit(child)

        print(while_scope)
        print("LEAVE scope: while")
        self.current_scope = self.current_scope.enclosing_scope

    def visit_VarDecl(self, node):
        type_name = node.type_node.value
        type_symbol = self.current_scope.lookup(type_name)

        var_name = node.var_node.value
        var_symbol = VarSymbol(var_name, type_symbol)

        if(self.current_scope.lookup(var_name, current_scope_only=True) is not None):
            raise NameError("Cannot initialize a variable that has already been declared '" + str(var_name) + "' on line: " + str(node.token.get_line()))

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

        right = self.visit(node.right)

    def visit_Identifier(self, node):
        var_name = node.value
        var_symbol = self.current_scope.lookup(var_name)

        if(var_symbol is None):
            raise NameError(repr(var_name) + " is an undefined variable on line: " + str(node.token.get_line()))

        return var_symbol


    def analyze(self):
        tree = self.parser.parse()
        self.visit(tree)
