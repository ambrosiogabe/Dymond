from TokenType import TokenType

class NodeVisitor(object):
    def visit(self, node):
        method_name = "visit_" + type(node).__name__
        visitor = getattr(self, method_name, self.generic_visit)
        return visitor(node)

    def generic_visit(self, node):
        raise SyntaxError("No visit_{} method".format(type(node).__name__))


class Symbol(object):
    def __init__(self, name, type=None):
        self.name = name
        self.type = type

class BuiltInTypeSymbol(Symbol):
    def __init__(self, name):
        super(BuiltInTypeSymbol, self).__init__(name)

    def __str__(self):
        return self.name

    __repr__ = __str__

class VarSymbol(Symbol):
    def __init__(self, name, type):
        super(VarSymbol, self).__init__(name, type)

    def __str__(self):
        return "<{name}:{type}>".format(name=self.name, type=self.type)

    __repr__ = __str__

class AST(object):
  pass

class BinOp(AST):
     def __init__(self, left, op, right):
         self.left = left
         self.token = self.op = op
         self.right = right

class Integer(AST):
    def __init__(self, token):
        self.token = token

class Float(AST):
    def __init__(self, token):
        self.token = token

class UnaryOperator(AST):
    def __init__(self, op, expr):
        self.token = self.op = op
        self.expr = expr

# These are the program properties
class Program(AST):
    def __init__(self, block):
        self.block = block

class Block(AST):
    def __init__(self, declarations, compound_statement):
        self.declarations = declarations
        self.compound_statement = compound_statement

class VarDecl(AST):
    def __init__(self, var_node, type_node):
        self.var_node = var_node
        self.type_node = type_node

class Type(AST):
    def __init__(self, token):
        self.token = token
        self.value = token.get_value()
