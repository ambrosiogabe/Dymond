from TokenType import TokenType
from AST_Nodes import NodeVisitor, VarSymbol,BuiltInTypeSymbol
from collections import OrderedDict

class SymbolTable(object):
    def __init__(self):
        self._symbols = OrderedDict()
        self._init_builtins()

    def _init_builtins(self):
        self.define(BuiltInTypeSymbol(TokenType.INTEGER_TYPE))
        self.define(BuiltInTypeSymbol(TokenType.STRING_TYPE))
        self.define(BuiltInTypeSymbol(TokenType.DECIMAL_TYPE))

    def __str__(self):
        s = "Symbol Table Contents\nSymbols: {symbols}".format(
            symbols = [value for value in self._symbols.values()]
        )
        return s

    __repr__ = __str__

    def define(self, symbol):
        print("Define: %s" % symbol)
        self._symbols[symbol.name] = symbol

    def lookup(self, name):
        print("Lookup: %s" % name)
        symbol = self._symbols.get(name)
        return symbol

class SymbolTableBuilder(NodeVisitor):
    def __init__(self):
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
        self.symtab.define(var_symbol)

    def visit_Variable(self, node):
        for var in node.var_decls:
            self.visit(var)

    def visit_Assign(self, node):
        var_name = node.left.value
        var_symbol = self.symtab.lookup(var_name)
        if(var_symbol is None):
            raise NameError(repr(var_name) + " is not found on line: " + str(node.token.get_line()))

        self.visit(node.right)

    def visit_Identifier(self, node):
        var_name = node.value
        var_symbol = self.symtab.lookup(var_name)

        if(var_symbol is None):
            raise NameError(repr(var_name) + " is not found on line: " + str(node.token.get_line()))
