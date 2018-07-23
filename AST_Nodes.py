from TokenType import TokenType

class NodeVisitor(object):
    def visit(self, node):
        method_name = "visit_" + type(node).__name__
        visitor = getattr(self, method_name, self.generic_visit)
        return visitor(node)

    def generic_visit(self, node):
        raise SyntaxError("No visit_{} method".format(type(node).__name__))

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

class String(AST):
    def __init__(self, token):
        self.token = token

class Bool(AST):
    def __init__(self, token):
        self.token = token

class UnaryOperator(AST):
    def __init__(self, op, expr):
        self.token = self.op = op
        self.expr = expr

class IfNode(AST):
    def __init__(self, validity, true_block, false_block=None):
        self.validity = validity
        self.true_block = true_block
        self.false_block = false_block

class WhileNode(AST):
    def __init__(self, condition, true_block):
        self.condition = condition
        self.true_block = true_block

# These are the program properties
class Program(AST):
    def __init__(self, children):
        self.children = children

class Block(AST):
    def __init__(self, declarations, compound_statement):
        self.declarations = declarations
        self.compound_statement = compound_statement

class VarDecl(AST):
    def __init__(self, var_node, type_node, assign_node=None):
        self.var_node = var_node
        self.type_node = type_node
        self.token = var_node.token
        self.assign_node = assign_node


# NOT SURE IF I'M GOING TO KEEP THIS
class ParamDecl(AST):
    def __init__(self, param_node, type_node, assign_node=None):
        self.param_node = var_node
        self.type_node = type_node
        self.token = var_node.token
        self.assign_node = assign_node

class Variable(AST):
    def __init__(self, var_decls):
        self.var_decls = var_decls

class Type(AST):
    def __init__(self, token):
        self.token = token
        self.value = token.get_value()

class FuncDecl(AST):
    # func_values_node contains return type, parameters
    def __init__(self, func_name, return_type, parameters, children, return_node):
        self.func_name = func_name
        self.return_type = return_type
        self.parameters = parameters
        self.children = children
        self.return_node = return_node

class FunctionCall(AST):
    def __init__(self, func_name, parameters):
        self.func_name = func_name
        self.parameters = parameters














# Native Functions Nodes
class Print(AST):
    def __init__(self, param):
        self.param = param

class ToString(AST):
    def __init__(self, param):
        self.param = param

class Input(AST):
    def __init__(self, param):
        self.param = param

class ToInt(AST):
    def __init__(self, param):
        self.param = param

class ToDecimal(AST):
    def __init__(self, param):
        self.param = param
