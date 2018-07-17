from TokenType import TokenType

class Utils:
    @classmethod
    def get_all_tokens(cls):
        tokens =  {}
        for k, v in vars(TokenType).items():
            if(not k.startswith("__")):
                tokens[k] = v
        return tokens

    @classmethod
    def is_white_space(cls, symbol):
        return symbol == " " or symbol == "\t" \
        or symbol == "\r" or symbol == "\f" or symbol == "\b"

    @classmethod
    def is_beginning_of_literal(cls, symbol):
        return Utils.is_letter(symbol) or \
        Utils.is_beginning_of_identifier(symbol) or Utils.is_beginning_of_number(symbol) \
        or Utils.is_beginning_of_string(symbol)

    @classmethod
    def is_letter(cls, symbol):
        return ord(symbol) >= ord("a") and ord(symbol) <= ord("z") or \
        ord(symbol) >= ord("A") and ord(symbol) <= ord("Z")

    @classmethod
    def is_letter_or_digit(cls, symbol):
        return Utils.is_letter(symbol) or ord(symbol) >= ord("0") and \
        ord(symbol) <= ord("9")

    @classmethod
    def is_operator(cls, symbol):
        return symbol == "+" or symbol == "-" or symbol == "*" or symbol == "/" \
        or symbol == "=" or symbol == ">" or symbol == "<" or symbol == "!" \
        or symbol == "&" or symbol == "|" or symbol == "%" or symbol == "~" \
        or symbol == "$" or symbol == "^"

    @classmethod
    def is_delimeter(cls, symbol):
        return symbol == "{" or symbol == "}" or symbol == "[" or \
        symbol == "]" or symbol == "(" or symbol == ")" or \
        symbol == ":" or symbol == "," or symbol == ";" or symbol == "#"

    @classmethod
    def is_identifier_part(cls, symbol):
        return symbol == "_" or Utils.is_letter_or_digit(symbol)

    @classmethod
    def is_dot(cls, symbol):
        return symbol == "."

    @classmethod
    def is_newline(cls, symbol):
        return symbol == "\n"

    @classmethod
    def is_beginning_of_identifier(cls, symbol):
        return Utils.is_letter(symbol) or symbol == "_"

    @classmethod
    def is_digit(cls, symbol):
        return ord(symbol) >= ord("0") and ord(symbol) <= ord("9")

    @classmethod
    def is_beginning_of_number(cls, symbol):
        return Utils.is_digit(symbol) or symbol == "."

    @classmethod
    def is_beginning_of_string(cls, symbol):
        return symbol == '"'

    @classmethod
    def is_string_delimiter(cls, symbol):
        return symbol == "\""

    @classmethod
    def is_escape_character(cls, symbol):
        return symbol == "\\"

    @classmethod
    def is_end_of_escape_sequence(cls, symbol):
        return symbol == '"' or symbol== "\\" or symbol == "n" \
        or symbol == "r" or symbol == "t" or symbol == "b" \
        or symbol == "f"

    @classmethod
    def is_null_char(cls, symbol):
        return symbol == chr(200)

    @classmethod
    def null_char(cls):
        return chr(200)
