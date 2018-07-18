from TokenType import TokenType
from AST_Nodes import NodeVisitor
from collections import OrderedDict

class Symbol(object):
    def __init__(self, name, type=None):
        self.name = name
        self.type = type
        #self.category = category

class BuiltInTypeSymbol(Symbol):
    def __init__(self, name):
        super(BuiltInTypeSymbol, self).__init__(name)

    def __str__(self):
        return self.name

    def __repr__(self):
        return "<{class_name}(name='{name}')>".format(
        class_name = self.__class__.__name__,
        name = self.name
        )

class VarSymbol(Symbol):
    def __init__(self, name, type):
        super(VarSymbol, self).__init__(name, type)

    def __str__(self):
        return "<{class_name}(name='{name}', type='{type}')>".format(
        class_name = self.__class__.__name__,
        name=self.name,
        type=self.type
        )

    __repr__ = __str__




class SymbolTable(object):
    def __init__(self):
        self._symbols = OrderedDict()
        self._init_builtins()

    def _init_builtins(self):
        self.define(BuiltInTypeSymbol(TokenType.INTEGER_TYPE))
        self.define(BuiltInTypeSymbol(TokenType.STRING_TYPE))
        self.define(BuiltInTypeSymbol(TokenType.DECIMAL_TYPE))

    def __str__(self):
        symtab_header = "Symbol table contents"
        lines = ['\n', symtab_header, '_' * len(symtab_header)]
        lines.extend(
            ("%8s: %r" % (key, value))
            for key, value in self._symbols.items()
        )
        lines.append('\n')
        s = '\n'.join(lines)
        return s

    __repr__ = __str__

    def define(self, symbol):
        print("Define: %s" % symbol)
        self._symbols[symbol.name] = symbol

    def lookup(self, name):
        print("Lookup: %s" % name)
        symbol = self._symbols.get(name)
        return symbol

    def insert(self, symbol):
        print("Insert: %s" % symbol.name)
        self._symbols[symbol.name] = symbol
