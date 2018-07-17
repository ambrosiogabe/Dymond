from TokenType import TokenType
from AST_Nodes import *

class CompoundStatement(object):
    def __init__(self):
        self.children = []

class Assign(object):
    # Left is the variable name
    # operator is the = sign
    # Right is the value to assign to the operand
    # Ex. a = 4
    def __init__(self, left, op, right):
        self.left = left
        self.token = self.op = op
        self.right = right

class Identifier(object):
    # The identifier node is constructed out of id token
    def __init__(self, token):
        self.token = token
        self.value = token.get_value()

class EmptyProgram(object):
    def __init__(self):
        pass





class Parser(object):
    def __init__(self, lexer):
        self.lexer = lexer
        self.current_token = self.lexer.next_token()

    def eat(self, token_type):
        if(self.current_token.get_type() == token_type):
            self.current_token = self.lexer.next_token()
        else:
            if(type(token_type).__name__ != "TokenType"):
                raise SyntaxError("Invalid syntax: " + "Expected: " + token_type + " Instead got: " + self.current_token.get_value() + \
                 " on line: " + str(self.current_token.get_line()))

            raise SyntaxError("Invalid syntax: " + "Expected: " + token_type.get_value() + " Instead got: " + self.current_token.get_value() + \
             " on line: " + str(self.current_token.get_line()))


    def program(self):
        # program : block
        self.eat(TokenType.BOF)
        node = self.block()
        self.eat(TokenType.EOF)

        program = Program(node)
        return program


    def block(self):
        # block : declarations compound_statement
        declaration_nodes = self.declarations()
        compound_statement = self.compound_statement()
        node = Block(declaration_nodes, compound_statement)
        return node

    def declarations(self):
        # declarations : variable(SEMI_COLON)+
        types = (TokenType.INTEGER_TYPE, TokenType.DECIMAL_TYPE, TokenType.STRING_TYPE)
        declarations = []

        while(self.current_token.get_type() in types):
            type = self.current_token.get_type()
            type_node = Type(self.current_token)
            self.eat(self.current_token.get_type())

            if(not self.current_token.get_type() == TokenType.DOT):
                var_decl = self.variable_declaration(type_node)
                declarations.extend(var_decl)

        return declarations

    def variable_declaration(self, type_node):
        # variable_declaration : type_spec ID SEMI_COLON
        var_nodes = [Identifier(self.current_token)]
        self.eat(TokenType.IDENTIFIER)

        while(self.current_token.get_type() == TokenType.COMMA):
            self.eat(TokenType.COMMA)
            var_nodes.append(Identifier(self.current_token))
            self.eat(TokenType.IDENTIFIER)

        self.eat(TokenType.SEMI_COLON)

        var_declarations = [
            VarDecl(var_node, type_node)
            for var_node in var_nodes
        ]
        return var_declarations

    def compound_statement(self):
        nodes = self.statement_list()

        root = CompoundStatement()
        for node in nodes:
            root.children.append(node)

        if(type(nodes[0]).__name__ == "NoneType"):
            root.children[0] = EmptyProgram()

        return root

    def statement_list(self):
        """
        statement_list : statement
                       | statement SEMI_COLON statement_list
        """
        node = self.statement()

        results = [node]

        while(self.current_token.get_type() == TokenType.SEMI_COLON):
            self.eat(TokenType.SEMI_COLON)
            node = self.statement()

            # This if statement checks in case we have reach EOF
            if(node):
                results.append(node)

        if(self.current_token.get_type() == TokenType.IDENTIFIER):
            raise SyntaxError("Did not expect an identifier at line: ", self.current_token.get_line())

        return results

    def statement(self):
        """
        statement : assignment_statement
                  | empty
        """
        node = None
        if(self.current_token.get_type() == TokenType.IDENTIFIER):
            node = self.assignment_statement()
        elif(self.current_token.get_type() == TokenType.DECIMAL or self.current_token.get_type() == TokenType.INTEGER):
            node = self.expr()
        elif(self.current_token.get_type() == TokenType.EOF):
            return

        return node

    def assignment_statement(self):
        """
        assignment_statement : variable ASSIGN expr
        """

        left = self.variable()
        token = self.current_token
        self.eat(TokenType.EQUAL)
        right = self.expr()
        node = Assign(left, token, right)
        return node

    def variable(self):
        """
        variable : ID
        """

        node = Identifier(self.current_token)
        self.eat(TokenType.IDENTIFIER)
        return node


    def expr(self):
        node = self.term()

        while(self.current_token.get_type() in (TokenType.PLUS, TokenType.MINUS)):
            token = self.current_token
            if(token.get_type() == TokenType.PLUS):
                self.eat(TokenType.PLUS)
            elif(token.get_type() == TokenType.MINUS):
                self.eat(TokenType.MINUS)

            node = BinOp(left=node, op=token, right=self.term())

        return node


    def term(self):
        node = self.factor()

        while(self.current_token.get_type() in (TokenType.DIV, TokenType.TIMES, TokenType.INTEGER_DIV)):
            token = self.current_token
            if(token.get_type() == TokenType.TIMES):
                self.eat(TokenType.TIMES)
            elif(token.get_type() == TokenType.DIV):
                self.eat(TokenType.DIV)
            elif(token.get_type() == TokenType.INTEGER_DIV):
                self.eat(TokenType.INTEGER_DIV)

            node = BinOp(left=node, op=token, right=self.factor())

        return node

    def factor(self):
        token = self.current_token
        if(token.get_type() == TokenType.PLUS):
            self.eat(TokenType.PLUS)
            return UnaryOperator(token, self.factor())
        elif(token.get_type() == TokenType.MINUS):
            self.eat(TokenType.MINUS)
            return UnaryOperator(token, self.factor())
        elif(token.get_type() == TokenType.INTEGER):
            self.eat(TokenType.INTEGER)
            return Integer(token)
        elif(token.get_type() == TokenType.DECIMAL):
            self.eat(TokenType.DECIMAL)
            return Float(token)
        elif(token.type == TokenType.LEFT_PAREN):
            self.eat(TokenType.LEFT_PAREN)
            node = self.expr()
            self.eat(TokenType.RIGHT_PAREN)
            return node
        else:
            node = self.variable()
            return node


    def parse(self):
        node = self.program()
        if(self.current_token.get_type() != TokenType.EOF):
            raise SyntaxError("EOF Error: Never reached end of file. Stopped at line: " + self.current_token.get_line() + " on token: " + self.current_token.get_value() )
        return node
