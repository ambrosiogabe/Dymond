from TokenType import TokenType
from AST_Nodes import *
from Token import Token

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
    def __init__(self, lexer, program_name=None):
        self.lexer = lexer
        self.current_token = self.lexer.next_token()
        self.program_name = program_name

    def eat(self, token_type):
        if(self.current_token.get_type() == token_type):
            self.current_token = self.lexer.next_token()
        else:
            if(type(token_type).__name__ != "TokenType"):
                raise SyntaxError("Invalid syntax: " + "Expected: '" + token_type + "' Instead got: '" + self.current_token.get_value() + \
                 "' on line: " + str(self.current_token.get_line()))

            raise SyntaxError("Invalid syntax: " + "Expected: '" + token_type.get_value() + "' Instead got: '" + self.current_token.get_value() + \
             "' on line: " + str(self.current_token.get_line()))


    def program(self):
        # program : Declare*
        #         | Statement* EOF
        self.eat(TokenType.BOF)

        children = []
        while(self.current_token.get_type() != TokenType.EOF):

            if(self.current_token.get_type() in TokenType.ALL_TYPES):
                children.append(self.variable_declarations())
            elif(self.current_token.get_type() == TokenType.FUNC):
                children.append(self.function_declaration())
            elif(self.current_token.get_type() == TokenType.IF):
                children.append(self.if_statement())
            elif(self.current_token.get_type() == TokenType.WHILE):
                children.append(self.while_loop())
            elif(self.current_token.get_type() != TokenType.EOF):
                children.append(self.compound_statement())

        self.eat(TokenType.EOF)

        program = Program(children)
        return program


    def block(self):
        """ block : LEFT_BRACE declarations*
                  | compound_statement* RIGHT_BRACE
        """
        children = []

        while(self.current_token.get_type() != TokenType.RIGHT_BRACE):
            child = None
            if(self.current_token.get_type() in TokenType.ALL_TYPES):
                children.append(self.variable_declarations())
            elif(self.current_token.get_type() == TokenType.FUNC):
                raise SyntaxError("Cannot declare a function inside a block. Fix line: " + str(self.current_token.get_line()))
            elif(self.current_token.get_type() != TokenType.RIGHT_BRACE):
                children.append(self.compound_statement())
            elif(self.current_token.get_type() == TokenType.RIGHT_BRACE):
                break

        return children

    def variable_declarations(self):
        """declaration : variable(SEMI_COLON)
        """

        type = self.current_token.get_type()
        type_node = Type(self.current_token)
        self.eat(self.current_token.get_type())

        var_decl = Variable(self.variable_declaration(type_node))

        return var_decl

    def function_declaration(self):
        """ function_declarations : function
                                  | (FUNC ID FUNC_VALUES BLOCK)
        """
        # Declaring function
        self.eat(TokenType.FUNC)

        # Return type of the current function
        return_type = self.current_token.get_type()

        if(return_type in TokenType.ALL_TYPES):
            self.eat(self.current_token.get_type())

        # Function identifier
        func_name = self.current_token.get_value()
        self.eat(TokenType.IDENTIFIER)

        # Function parameters
        parameters = []
        self.eat(TokenType.LEFT_PAREN)
        while(self.current_token.get_type() != TokenType.RIGHT_PAREN):
            type = self.current_token.get_type()
            type_node = Type(self.current_token)

            self.eat(self.current_token.get_type())

            param_decl = self.parameter_declaration(type_node)
            parameters.extend(param_decl)

            if(self.current_token.get_type() != TokenType.RIGHT_PAREN):
                    self.eat(TokenType.COMMA)

        self.eat(TokenType.RIGHT_PAREN)

        self.eat(TokenType.LEFT_BRACE)
        children = self.block()
        self.eat(TokenType.RIGHT_BRACE)

        func_decl = FuncDecl(func_name, return_type, parameters, children)

        return func_decl

    def if_statement(self):
        self.eat(TokenType.IF)

        validity = self.expr()

        self.eat(TokenType.LEFT_BRACE)
        true_block = self.block()
        self.eat(TokenType.RIGHT_BRACE)

        false_block = None

        if(self.current_token.get_type() == TokenType.ELSE):
            self.eat(TokenType.ELSE)

            self.eat(TokenType.LEFT_BRACE)
            false_block = self.block()
            self.eat(TokenType.RIGHT_BRACE)

        return IfNode(validity, true_block, false_block)

    def while_loop(self):
        self.eat(TokenType.WHILE)
        condition = self.expr()

        self.eat(TokenType.LEFT_BRACE)
        true_block = self.block()
        self.eat(TokenType.RIGHT_BRACE)

        return WhileNode(condition, true_block)


    def parameter_declaration(self, type_node):
        var_node = Identifier(self.current_token)
        self.eat(TokenType.IDENTIFIER)

        var_decl = [VarDecl(var_node, type_node)]

        return var_decl

    def variable_declaration(self, type_node):
        # variable_declaration : type_spec ID,ID,* SEMI_COLON
        #                      | type_spec ID = EXPR SEMI_COLON
        id = self.current_token
        var_nodes = [Identifier(self.current_token)]
        self.eat(TokenType.IDENTIFIER)

        while(self.current_token.get_type() == TokenType.COMMA):
            self.eat(TokenType.COMMA)
            var_nodes.append(Identifier(self.current_token))
            self.eat(TokenType.IDENTIFIER)

        if(self.current_token.get_type() == TokenType.EQUAL):
            token = self.current_token
            self.eat(TokenType.EQUAL)
            var_declarations = []
            for var_node in var_nodes:
                var_declarations.append(VarDecl(var_node, type_node, self.decl_and_assignment_statement(var_node, token)))
                if(self.current_token.get_type() != TokenType.SEMI_COLON):
                    self.eat(TokenType.COMMA)
            self.eat(TokenType.SEMI_COLON)
            return var_declarations

        self.eat(TokenType.SEMI_COLON)

        var_declarations = [
            VarDecl(var_node, type_node)
            for var_node in var_nodes
        ]
        return var_declarations

    def decl_and_assignment_statement(self, var_node, token):
        """
        assignment_statement : variable ASSIGN expr
        """

        right = self.expr()
        node = Assign(var_node, token, right)
        return node

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
            if(self.current_token.get_type() in TokenType.ALL_TYPES):
                break
            node = self.statement()

            # This if statement checks in case we have reach EOF
            if(node):
                results.append(node)

        if(self.current_token.get_type() == TokenType.IDENTIFIER):
            raise SyntaxError("Did not expect an identifier at line: " + str(self.current_token.get_line()))

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


        if(self.current_token.get_type() == TokenType.EQUAL):
            self.eat(TokenType.EQUAL)
            right = self.expr()
        elif(self.current_token.get_type() == TokenType.PLUS_EQUAL):
            self.eat(TokenType.PLUS_EQUAL)
            right = self.expr_binop(left, Token(TokenType.PLUS, TokenType.PLUS, token.get_line(), token.get_line()) )
        elif(self.current_token.get_type() == TokenType.MINUS_EQUAL):
            self.eat(TokenType.MINUS_EQUAL)
            right = self.expr_binop(left, Token(TokenType.MINUS, TokenType.MINUS, token.get_line(), token.get_line()) )
        elif(self.current_token.get_type() == TokenType.DIV_EQUAL):
            self.eat(TokenType.DIV_EQUAL)
            right = self.expr_binop(left, Token(TokenType.DIV, TokenType.DIV, token.get_line(), token.get_line()) )
        elif(self.current_token.get_type() == TokenType.CARET_EQUAL):
            self.eat(TokenType.CARET_EQUAL)
            right = self.expr_binop(left, Token(TokenType.CARET, TokenType.CARET, token.get_line(), token.get_line()) )
        elif(self.current_token.get_type() ==  TokenType.TIMES_EQUAL):
            self.eat(TokenType.TIMES_EQUAL)
            right = self.expr_binop(left, Token(TokenType.TIMES, TokenType.TIMES, token.get_line(), token.get_line()) )
        elif(self.current_token.get_type() == TokenType.MODULO_EQUAL):
            self.eat(TokenType.MODULO_EQUAL)
            right = self.expr_binop(left, Token(TokenType.MODULO, TokenType.MODULO, token.get_line(), token.get_line()) )
        elif(self.current_token.get_type() == TokenType.PLUS_PLUS):
            self.eat(TokenType.PLUS_PLUS)
            right = BinOp( left=left, op=token, right=Integer(Token( TokenType.INTEGER, "1", token.get_line(), token.get_line() ) ))
        elif(self.current_token.get_type() == TokenType.MINUS_MINUS):
            self.eat(TokenType.MINUS_MINUS)
            right = BinOp( left=left, op=token, right=Integer(Token( TokenType.INTEGER, "1", token.get_line(), token.get_line() ) ))
        else:
            raise SyntaxError("Expected one of the following: '" + TokenType.EQUAL + "' '" + TokenType.PLUS_EQUAL + "' '" + TokenType.MINUS_EQUAL + "' '" + TokenType.TIMES_EQUAL + "' '" + TokenType.DIV_EQUAL + "' '" + TokenType.CARET_EQUAL + "' '" + TokenType.MODULO_EQUAL + \
            "' instead got: '" + self.current_token.get_value() + "' on line: " + str(self.current_token.get_line()))


        node = Assign(left, token, right)
        return node

    def expr_binop(self, left, token):
        """ Deals with 1 += 2 type expressions
        """
        node = BinOp(left=left, op=token, right=self.expr())
        return node

    def variable(self):
        """
        variable : ID
        """

        node = Identifier(self.current_token)
        self.eat(TokenType.IDENTIFIER)
        return node


    # Or operator level: 6
    def expr(self):
        node = self.and_expr()

        while(self.current_token.get_type() == TokenType.OR):
            token = self.current_token
            self.eat(TokenType.OR)

            node = BinOp(left=node, op=token, right=self.and_expr())

        return node

    # and level: 5
    def and_expr(self):
        node = self.equality_expr()

        while(self.current_token.get_type() == TokenType.AND):
            token = self.current_token
            self.eat(TokenType.AND)
            node = BinOp(left=node, op=token, right=self.equality_expr())

        return node

    def equality_expr(self):
        node = self.additive()

        while(self.current_token.get_type() in (TokenType.DOUBLE_EQUAL, TokenType.NOT_EQUAL)):
            token = self.current_token
            if(token.get_type() == TokenType.DOUBLE_EQUAL):
                self.eat(TokenType.DOUBLE_EQUAL)
            elif(token.get_type() == TokenType.NOT_EQUAL):
                self.eat(TokenType.NOT_EQUAL)

            node = BinOp(left=node, op=token, right=self.additive())

        return node

    # Plus or minus level: 4
    def additive(self):
        node = self.term()

        while(self.current_token.get_type() in (TokenType.PLUS, TokenType.MINUS)):
            token = self.current_token
            if(token.get_type() == TokenType.PLUS):
                self.eat(TokenType.PLUS)
            elif(token.get_type() == TokenType.MINUS):
                self.eat(TokenType.MINUS)

            node = BinOp(left=node, op=token, right=self.term())


        return node

    # Division, multiplication, and integer division and modulo operators level: 3
    def term(self):
        node = self.exponent()

        while(self.current_token.get_type() in (TokenType.DIV, TokenType.TIMES, TokenType.INTEGER_DIV, TokenType.MODULO)):
            token = self.current_token
            if(token.get_type() == TokenType.TIMES):
                self.eat(TokenType.TIMES)
            elif(token.get_type() == TokenType.DIV):
                self.eat(TokenType.DIV)
            elif(token.get_type() == TokenType.INTEGER_DIV):
                self.eat(TokenType.INTEGER_DIV)
            elif(token.get_type() == TokenType.MODULO):
                self.eat(TokenType.MODULO)

            node = BinOp(left=node, op=token, right=self.exponent())

        return node

    # Exponent and unary ++ -- operator level: 2
    def exponent(self):
        node = self.relational_operators()

        while(self.current_token.get_type() in (TokenType.CARET, TokenType.PLUS_PLUS, TokenType.MINUS_MINUS)):
            token = self.current_token
            if(token.get_type() == TokenType.CARET):
                self.eat(TokenType.CARET)
                node = BinOp(left=node, op=token, right=self.relational_operators())
            elif(token.get_type() == TokenType.PLUS_PLUS):
                self.eat(TokenType.PLUS_PLUS)
                node = BinOp( left=node, op=token, right=Integer(Token( TokenType.INTEGER, "1", token.get_line(), token.get_line() ) ))
            elif(token.get_type() == TokenType.MINUS_MINUS):
                self.eat(TokenType.MINUS_MINUS)
                node = BinOp( left=node, op=token, right=Integer(Token( TokenType.INTEGER, "1", token.get_line(), token.get_line() ) ))

        return node

    def relational_operators(self):
        node = self.factor()

        while(self.current_token.get_type() in (TokenType.LESS, TokenType.GREATER, TokenType.LESS_OR_EQUAL, TokenType.GREATER_OR_EQUAL)):
            token = self.current_token
            if(token.get_type() == TokenType.LESS):
                self.eat(TokenType.LESS)
                node = BinOp(left=node, op=token, right=self.factor())

        return node

    # Unary operators and other operators of highest precedence
    def factor(self):
        token = self.current_token

        if(token.get_type() == TokenType.PLUS):
            self.eat(TokenType.PLUS)
            return UnaryOperator(token, self.factor())
        elif(token.get_type() == TokenType.MINUS):
            self.eat(TokenType.MINUS)
            return UnaryOperator(token, self.factor())
        elif(token.get_type() == TokenType.PLUS_PLUS):
            self.eat(TokenType.PLUS_PLUS)
            return UnaryOperator(token, self.factor())
        elif(token.get_type() == TokenType.MINUS_MINUS):
            self.eat(TokenType.MINUS_MINUS)
            return UnaryOperator(token, self.factor())
        elif(token.get_type() == TokenType.NOT):
            self.eat(TokenType.NOT)
            return UnaryOperator(token, self.factor())
        elif(token.get_type() == TokenType.INTEGER):
            self.eat(TokenType.INTEGER)
            return Integer(token)
        elif(token.get_type() == TokenType.DECIMAL):
            self.eat(TokenType.DECIMAL)
            return Float(token)
        elif(token.get_type() == TokenType.BOOL):
            self.eat(TokenType.BOOL)
            return Bool(token)
        elif(token.get_type() == TokenType.LEFT_PAREN):
            self.eat(TokenType.LEFT_PAREN)
            node = self.expr()
            self.eat(TokenType.RIGHT_PAREN)
            return node
        elif(token.get_type() == TokenType.IDENTIFIER):
            node = self.variable()
            return node
        elif(token.get_type() == TokenType.STRING):
            self.eat(TokenType.STRING)
            return String(token)

        raise SyntaxError("Error, unrecognized token: '" + str(token.get_value()) + "' on line: " + str(token.get_line()))


    def parse(self):
        node = self.program()
        if(self.current_token.get_type() != TokenType.EOF):
            raise SyntaxError("EOF Error: Never reached end of file. Stopped at line: " + self.current_token.get_line() + " on token: " + self.current_token.get_value() )
        return node
