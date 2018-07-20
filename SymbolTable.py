from TokenType import TokenType
from AST_Nodes import NodeVisitor
from collections import OrderedDict

class Symbol(object):
    def __init__(self, name, type=None):
        self.name = name
        self.type = type

class BuiltInTypeSymbol(Symbol):
    def __init__(self, name, type):
        super(BuiltInTypeSymbol, self).__init__(name, type)

    def __str__(self):
        return self.name

    def __repr__(self):
        return "<{class_name}(name='{name}')>".format(
        class_name = self.__class__.__name__,
        name = self.name
        )

    __repr__ = __str__

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

class FunctionSymbol(Symbol):
    def __init__(self, name, params=None):
        super(FunctionSymbol, self).__init__(name)
        self.params = params if params is not None else []

    def __str__(self):
        return '<{class_name}(name={name}, parameters={params})>'.format(
            class_name=self.__class__.__name__,
            name=self.name,
            params=self.params
        )

    __repr__ = __str__



class SymbolTable(object):
    def __init__(self, scope_name, scope_level, enclosing_scope=None):
        self._symbols = OrderedDict()
        self.scope_name = scope_name
        self.scope_level = scope_level
        self.enclosing_scope = enclosing_scope

        if(enclosing_scope is None):
            self._init_builtins()

    def _init_builtins(self):
        self.define(BuiltInTypeSymbol(TokenType.INTEGER, TokenType.INTEGER))
        self.define(BuiltInTypeSymbol(TokenType.STRING, TokenType.STRING))
        self.define(BuiltInTypeSymbol(TokenType.DECIMAL, TokenType.DECIMAL))
        self.define(BuiltInTypeSymbol(TokenType.BOOL, TokenType.BOOL))

    def __str__(self):
        symtab_header = "SCOPE (SCOPED SYMBOL TABLE)"
        lines = ['\n', symtab_header, '=' * len(symtab_header)]
        for header_name, header_value in (
            ('Scope name', self.scope_name),
            ('Scope level', self.scope_level),
            ('Enclosing scope',
                self.enclosing_scope.scope_name if self.enclosing_scope else None
            )
        ):
            lines.append("%-15s: %s" % (header_name, header_value))
        h2 = "Scope (Scoped symbol table) contents"
        lines.extend([h2, '-' * len(h2)])
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

    def lookup(self, name, current_scope_only=False):
        print("Lookup: %s. (Scope name: %s)" % (name, self.scope_name))
        symbol = self._symbols.get(name)

        if(symbol is not None):
            return symbol

        if(self.enclosing_scope is not None and not current_scope_only):
            return self.enclosing_scope.lookup(name)

    def insert(self, symbol):
        print("Insert: %s" % symbol.name)
        self._symbols[symbol.name] = symbol
