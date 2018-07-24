from TokenType import TokenType
from AST_Nodes import NodeVisitor
from collections import OrderedDict

class Symbol(object):
    def __init__(self, name, type=None):
        self.name = name
        self.type = type
        self.value = None

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

    def __call__(self, var):
        return self.name

    __repr__ = __str__

class VarSymbol(Symbol):
    def __init__(self, name, type):
        super(VarSymbol, self).__init__(name, type)

    def __str__(self):
        return "<{class_name}(name='{name}', type='{type}')> = {value}".format(
        class_name = self.__class__.__name__,
        name=self.name,
        type=self.type,
        value=self.value
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
        self.children = OrderedDict()

        self.reset_multi_scope_vars()

        if(enclosing_scope is None):
            self._init_builtins()

    def _init_builtins(self):
        self.define(BuiltInTypeSymbol(TokenType.INTEGER, TokenType.INTEGER))
        self.define(BuiltInTypeSymbol(TokenType.STRING, TokenType.STRING))
        self.define(BuiltInTypeSymbol(TokenType.DECIMAL, TokenType.DECIMAL))
        self.define(BuiltInTypeSymbol(TokenType.BOOL, TokenType.BOOL))
        self.define(BuiltInTypeSymbol(TokenType.LIST, TokenType.LIST))
        self.define(FunctionSymbol("print", VarSymbol(name="x", type="String")))

    def __str__(self):
        children = ""
        for key in self.children:
            children += key + " "

        symtab_header = "SCOPE (SCOPED SYMBOL TABLE)"
        lines = ['\n', symtab_header, '=' * len(symtab_header)]
        for header_name, header_value in (
            ('Scope name', self.scope_name),
            ('Scope level', self.scope_level),
            ('Enclosing scope',
                self.enclosing_scope.scope_name if self.enclosing_scope else None
            ),
            ("Children: ", children)
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
        self._symbols[symbol.name] = symbol

    def lookup(self, name, current_scope_only=False):
        symbol = self._symbols.get(name)

        if(symbol is not None):
            return symbol

        if(self.enclosing_scope is not None and not current_scope_only):
            return self.enclosing_scope.lookup(name)

    def insert(self, symbol):
        self._symbols[symbol.name] = symbol

    def return_scope_of(self, name):
        symbol = self._symbols.get(name)

        if(symbol is not None):
            return self

        if(self.enclosing_scope is not None):
            return self.enclosing_scope.return_scope_of(name)

    def reset_multi_scope_vars(self):
        # These are scopes that can have multiple scopes in the same block
        # so I need to keep track of which scope I am at
        self.current_if = 0
        self.current_while = 0
        self.current_for = 0
        self.current_else = 0

        for child in self.children:
            self.children[child].reset_multi_scope_vars()
