from TokenType import TokenType
from Utils import Utils
from Token import Token

class Lexer:
    cur_pos = 0
    cur_line = 1
    cur_column = 1
    beginning_of_file = True

    def __init__(self, text):
        self.input = text

    def tokenize(self):
        tokens = []
        token = self.next_token()

        while token.get_value() != TokenType.EOF:
            tokens.append(token)
            token = self.next_token()

        eof = Token(TokenType.EOF, TokenType.EOF, self.cur_line + 2, 0)
        tokens.append(eof)
        return tokens

    def next_token(self):
        # Initialize the beginning of the file
        if(self.beginning_of_file):
            bof = Token(TokenType.BOF, TokenType.BOF, -1, -1)
            self.beginning_of_file = False
            return bof

        self.skip_white_spaces()

        if(self.cur_pos >= len(self.input)):
            return Token(TokenType.EOF, TokenType.EOF, self.cur_line, self.cur_column)

        symbol = self.input[self.cur_pos]

        if(Utils.is_beginning_of_literal(symbol)):
            return self.recognize_literal()

        if(Utils.is_operator(symbol)):
            return self.recognize_operator()

        if(Utils.is_delimeter(symbol)):
            return self.recognize_delimeter()

        if(Utils.is_dot(symbol)):
            column = self.cur_column
            self.cur_pos += 1
            self.cur_column += 1

            return Token(TokenType.DOT, ".", self.cur_line, column)

        if(Utils.is_newline(symbol)):
            line = self.cur_line
            column = self.cur_column

            self.cur_line += 1
            self.cur_column = 0
            self.cur_pos += 1

            return self.next_token()

        raise SyntaxError("Unrecognized token: " + symbol + " on line: " + str(self.cur_line))

    def recognize_literal(self):
        symbol = self.input[self.cur_pos]

        if(Utils.is_letter(symbol)):
            return self.recognize_keyword_or_identifier()

        if(Utils.is_beginning_of_identifier(symbol)):
            return self.recognize_identifier()

        if(Utils.is_beginning_of_number(symbol)):
            return self.recognize_number()

        if(Utils.is_beginning_of_string(symbol)):
            return self.recognize_string()

        return None

    def recognize_string(self):
        self.cur_pos += 1
        self.cur_column += 1

        in_escape_sequence = False
        symbol = self.input[self.cur_pos]
        entire_string = "\""

        while(self.cur_pos < len(self.input) and not(Utils.is_beginning_of_string(symbol) or in_escape_sequence)):
            symbol = self.input[self.cur_pos]

            if(Utils.is_escape_character(symbol) and not in_escape_sequence):
                in_escape_sequence = True
                self.cur_pos += 1
                self.cur_column += 1
                continue

            if(in_escape_sequence):
                if(Utils.is_end_of_escape_sequence(symbol)):
                    in_escape_sequence = False
                    if(symbol == "\\"):
                        entire_string += "\\"
                        break
                    elif(symbol == '"'):
                        entire_string += "\""
                        break
                    elif(symbol == "n"):
                        entire_string += "\n"
                        break
                    elif(symbol == "r"):
                        entire_string += "\r"
                        brea
                    elif(symbol == "t"):
                        entire_string += "\t"
                        break
                    elif(symbol == "b"):
                        entire_string += "\b"
                        break
                    elif(symobl == "f"):
                        entire_string += "\f"
                        break
                else:
                    raise SyntaxError("Unrecognized escape sequence, " +"\\" + symbol + ", on line: " + str(self.cur_line))
            else:
                entire_string += symbol
            self.cur_pos += 1
            self.cur_column += 1

        return Token(TokenType.STRING, entire_string, self.cur_pos, self.cur_column)

    def recognize_number(self):
        symbol = self.input[self.cur_pos]
        lookahead = self.input[self.cur_pos + 1] if self.cur_pos + 1 < len(self.input) else Utils.null_char()
        self.cur_pos += 1
        self.cur_column += 1

        if( (not Utils.is_null_char(lookahead) and symbol == "." and Utils.is_letter(lookahead)) or Utils.is_null_char(lookahead) and symbol == "."):
            return Token(TokenType.DOT, ".", self.cur_pos, self.cur_column)

        whole_number = "" + symbol
        previous_symbol = symbol

        while(self.cur_pos < len(self.input) and not Utils.is_white_space(self.input[self.cur_pos]) and not Utils.is_newline(self.input[self.cur_pos]) \
              and (not Utils.is_operator(self.input[self.cur_pos]) or (self.input[self.cur_pos] == "-") or (self.input[self.cur_pos] == "+")) and \
              not Utils.is_delimeter(self.input[self.cur_pos]) ):
            cur_symbol = self.input[self.cur_pos]

            if( (cur_symbol == "+" or cur_symbol == "-") and not(previous_symbol == "e" or previous_symbol == "E")):
                break

            whole_number += cur_symbol
            previous_symbol = cur_symbol
            self.cur_pos += 1
            self.cur_column += 1

        is_scientific = False
        is_decimal = False
        for i in range(len(whole_number)):
            cur_symbol = whole_number[i]
            if(cur_symbol == "." and not is_decimal):
                is_decimal = True
                continue
            elif(cur_symbol == "." and is_decimal):
                raise SyntaxError("Too many .'s. Unrecognized number literal: " + whole_number + " on line: " + str(self.cur_line))

            if( (cur_symbol == "e" or cur_symbol == "E") and not is_scientific and i != len(whole_number) - 1):
                is_scientific = True
                continue
            elif( (cur_symbol == "e" or cur_symbol == "E") and is_scientific):
                raise SyntaxError("Too many e's. Unrecognized number literal: " + whole_number + " on line: " + str(self.cur_line))

            if(is_scientific and (cur_symbol == "-" or cur_symbol == "+")):
                continue

            if(not Utils.is_digit(cur_symbol)):
                raise SyntaxError("Unrecognized number literal: " + whole_number + " on line: " + str(self.cur_line))

        if(is_scientific or is_decimal):
            return Token(TokenType.DECIMAL, whole_number, self.cur_line, self.cur_pos)

        return Token(TokenType.INTEGER, whole_number, self.cur_line, self.cur_pos)


    def recognize_keyword_or_identifier(self):
        token = self.recognize_keyword()

        return token if token != None else self.recognize_identifier()


    def recognize_keyword(self):
        symbol = self.input[self.cur_pos]

        all_keywords = Utils.get_all_tokens()

        for k, v in all_keywords.items():
            cur_keyword = v

            if(cur_keyword[0] == symbol):
                token = self.recognize_token(cur_keyword)

                if(token != None):
                    offset = token.length()

                    if(self.cur_pos + offset < len(self.input) and Utils.is_identifier_part(self.input[self.cur_pos + offset])):
                        return None

                    self.cur_pos += offset
                    self.cur_column += offset

                    return token

        return None

    def recognize_identifier(self):
        identifier = ""

        while(self.cur_pos < len(self.input)):
            symbol = self.input[self.cur_pos]

            if(not Utils.is_identifier_part(symbol)):
                break

            identifier += symbol
            self.cur_pos += 1

        self.cur_column += len(identifier)

        return Token(TokenType.IDENTIFIER, identifier, self.cur_line, self.cur_column)


    def recognize_token(self, keyword):
        for i in range(len(keyword)):
            if(self.cur_pos + i < len(self.input)):
                if(self.input[self.cur_pos + i] != keyword[i]):
                    return None
            else:
                return None

        return Token(keyword, keyword, self.cur_line, self.cur_column)


    def recognize_delimeter(self):
        symbol = self.input[self.cur_pos]
        column = self.cur_column

        self.cur_pos += 1
        self.cur_column += 1

        if(symbol == "{"):
            return Token(TokenType.LEFT_BRACE, "{", self.cur_line, column)
        elif(symbol == "}"):
            return Token(TokenType.RIGHT_BRACE, "}", self.cur_line, column)
        elif(symbol == "["):
            return Token(TokenType.LEFT_BRACKET, "[", self.cur_line, column)
        elif(symbol == "]"):
            return Token(TokenType.RIGHT_BRACKET, "]", self.cur_line, column)
        elif(symbol == "("):
            return Token(TokenType.LEFT_PAREN, "(", self.cur_line, column)
        elif(symbol == ")"):
            return Token(TokenType.RIGHT_PAREN, ")", self.cur_line, column)
        elif(symbol == ","):
            return Token(TokenType.COMMA, ",", self.cur_line, column)
        elif(symbol == ":"):
            return Token(TokenType.COLON, ":", self.cur_line, column)
        elif(symbol == ";"):
            return Token(TokenType.SEMI_COLON, ";", self.cur_line, column)
        elif(symbol == "#"):
            self.skip_until_new_line()
            return self.next_token()
        else:
            raise SyntaxError("Unrecognized Token '" + symbol + "' at line " + str(self.cur_line))

    def recognize_operator(self):
        symbol = self.input[self.cur_pos]
        lookahead = self.input[self.cur_pos + 1] if self.cur_pos + 1 < len(self.input) else Utils.null_char()
        column = self.cur_column

        if(not Utils.is_null_char(lookahead) and (lookahead == "=" or lookahead == "&" or lookahead == "|" or lookahead == "-")):
            self.cur_pos += 1
            self.cur_column += 1

        self.cur_pos += 1
        self.cur_column += 1

        if(symbol == "="):
            return \
            Token(TokenType.DOUBLE_EQUAL, "==", self.cur_line, self.cur_column) \
            if (not Utils.is_null_char(lookahead) and lookahead == "=") else \
            Token(TokenType.EQUAL, "=", self.cur_line, self.cur_column)

        elif(symbol == "%"):
            return \
            Token(TokenType.MODULO_EQUAL, "%=", self.cur_line, self.cur_column) \
            if (not Utils.is_null_char(lookahead) and lookahead == "=") else \
            Token(TokenType.MODULO, "%", self.cur_line, self.cur_column)

        elif(symbol == "+"):
            return \
            Token(TokenType.PLUS_EQUAL, "+=", self.cur_line, self.cur_column) \
            if (not Utils.is_null_char(lookahead) and lookahead == "=") else \
            Token(TokenType.PLUS, "+", self.cur_line, self.cur_column)

        elif(symbol == "*"):
            return \
            Token(TokenType.TIMES_EQUAL, "*=", self.cur_line, self.cur_column) \
            if (not Utils.is_null_char(lookahead) and lookahead == "=") else \
            Token(TokenType.TIMES, "*", self.cur_line, self.cur_column)

        elif(symbol == ">"):
            return \
            Token(TokenType.GREATER_OR_EQUAL, ">=", self.cur_line, self.cur_column) \
            if (not Utils.is_null_char(lookahead) and lookahead == "=") else \
            Token(TokenType.GREATER, ">", self.cur_line, self.cur_column)

        elif(symbol == "!"):
            return \
            Token(TokenType.NOT_EQUAL, "!=", self.cur_line, self.cur_column) \
            if (not Utils.is_null_char(lookahead) and lookahead == "=") else \
            Token(TokenType.NOT, "!", self.cur_line, self.cur_column)

        elif(symbol == "~"):
            return \
            Token(TokenType.TILDE_EQUAL, "~=", self.cur_line, self.cur_column) \
            if (not Utils.is_null_char(lookahead) and lookahead == "=") else \
            Token(TokenType.TILDE, self.cur_line, self.cur_column)

        elif(symbol == "$"):
            return \
            Token(TokenType.DOLLAR_EQUAL, "$=", self.cur_line, self.cur_column) \
            if (not Utils.is_null_char(lookahead) and lookahead == "=") else \
            Token(TokenType.DOLLAR, "$", self.cur_line, self.cur_column)

        elif(symbol == "^"):
            return \
            Token(TokenType.CARET_EQUAL, "^=", self.cur_line, self.cur_column) \
            if (not Utils.is_null_char(lookahead) and lookahead == "=") else \
            Token(TokenType.CARET, "^", self.cur_line, self.cur_column)

        elif(symbol == "&"):
            if(not Utils.is_null_char(lookahead) and lookahead == "&"):
                return Token(TokenType.AND, "&&", self.cur_line, column)

            raise SyntaxError("Unrecognized token: " + symbol + " on line: " + str(self.cur_line))

        elif(symbol == "|"):
            if(not Utils.is_null_char(lookahead) and lookahead == "|"):
                return Token(TokenType.OR, "||", self.cur_line, column)

            raise SyntaxError("Unrecognized token: " + symbol + " on line: " + str(self.cur_line))

        elif(symbol == "/"):
            if(not Utils.is_null_char(lookahead)):
                if(lookahead == "="):
                    return Token(TokenType.DIV_EQUAL, "/=", self.cur_line, column)

                if(lookahead != "=" and lookahead != "/" and lookahead != "*"):
                    return Token(TokenType.DIV, "/", self.cur_line, column)

                if(lookahead == "/"):
                    self.cur_pos += 1
                    return Token(TokenType.INTEGER_DIV, "//", self.cur_line, column)

                if(lookahead == "*"):
                    self.skip_until_multi_comment_end();
                    return self.next_token()

            if(Utils.is_null_char(lookahead)):
                return Token(TokenType.DIV, "/", self.cur_line, column)

            raise SyntaxError("Unrecognized token: " + symbol + " on line: " + str(self.cur_line))

        elif(symbol == "<"):
            if(not Utils.is_null_char(lookahead)):
                if(lookahead != "=" and lookahead != "-"):
                    return Token(TokenType.LESS, "<", self.cur_line, column)

                if(lookahead == "="):
                    return Token(TokenType.LESS_OR_EQUAL, "<=", self.cur_line, column)

                if(lookahead == "-"):
                    return Token(TokenType.LEFT_ARROW, "<-", self.cur_line, column)

            if(Utils.is_null_char(lookahead)):
                return Token(TokenType.LESS, self.cur_line, column)

            raise SyntaxError("Unrecognized token: " + symbol + " on line: " + str(self.cur_line))

        elif(symbol == "-"):
            if(not Utils.is_null_char(lookahead)):
                if(lookahead == "="):
                    return Token(TokenType.MINUS_EQUAL, "-=", self.cur_line, column)

                if(lookahead == ">"):
                    return Token(TokenType.RIGHT_ARROW, "->", self.cur_line, column)
                else:
                    if(lookahead == "-"):
                        self.cur_pos -= 1
                    return Token(TokenType.MINUS, "-", self.cur_line, column)

            if(Utils.is_null_char(lookahead) or (lookahead != "=" and lookahead != ">")):
                return Token(TokenType.MINUS, "-", self.cur_line, column)

            raise SyntaxError("Unrecognized token: " + symbol + " on line: " + str(self.cur_line))

        else:
            raise SyntaxError("Unrecognized token: " + symbol + " on line: " + str(self.cur_line))


    def skip_until_new_line(self):
        if(self.cur_pos < len(self.input)):
            while(not Utils.is_newline(self.input[self.cur_pos]) and self.cur_pos < len(self.input)):
                self.cur_pos += 1
                if(self.cur_pos >= len(self.input)):
                    break

    def skip_until_multi_comment_end(self):
        if(self.cur_pos < len(self.input)):
            while(self.cur_pos < len(self.input)):
                symbol = self.input[self.cur_pos]
                lookahead = self.input[self.cur_pos + 1] if self.cur_pos + 1 < len(self.input) else Utils.null_char()

                if(symbol == "\n"):
                    self.cur_line += 1

                if(symbol == "*"):
                    if(not Utils.is_null_char(lookahead)):
                        if(lookahead == "/"):
                            self.cur_pos += 2
                            break
                self.cur_pos += 1


    def skip_white_spaces(self):
        if(self.cur_pos < len(self.input)):
            while(Utils.is_white_space(self.input[self.cur_pos])):
                self.cur_pos += 1
                if(self.cur_pos >= len(self.input)):
                    break
