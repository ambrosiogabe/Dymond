class TokenType:
    # Keywords
    ABSTRACT = "abstract"
    AS = "as"
    CLASS = "class"
    ELSE = "else"
    EXTENDS = "extends"
    FALSE = "False"
    FINAL = "final"
    FUNC = "func"
    FOR = "for"
    IF = "if"
    IN = "in"
    LAZY = "lazy"
    LET = "let"
    NEW = "new"
    NULL = "null"
    OVERRIDE = "override"
    PRIVATE = "private"
    PROTECTED = "protected"
    RETURN = "return"
    SUPER = "super"
    TO = "to"
    THIS = "this"
    TRUE = "True"
    VAR = "var"
    WHILE = "while"

    # Dispatch Operators
    DOT = "."

    # Boolean operators
    AND = "&&"
    OR = "||"
    NOT = "!"

    # Assignment Operators
    LEFT_ARROW = "<-"
    DIV_EQUAL = "/="
    EQUAL = "="
    MINUS_EQUAL = "-="
    MODULO_EQUAL = "%="
    PLUS_EQUAL = "+="
    RIGHT_ARROW = "->"
    TIMES_EQUAL = "*="

    # Arithmetic Operators
    DIV = "/"
    MODULO = "%"
    MINUS = "-"
    PLUS = "+"
    TIMES = "*"
    INTEGER_DIV = "//"
    PLUS_PLUS = "++"
    MINUS_MINUS = "--"

    # Comparison Operators
    DOUBLE_EQUAL = "=="
    GREATER = ">"
    GREATER_OR_EQUAL = ">="
    LESS = "<"
    LESS_OR_EQUAL = "<="
    NOT_EQUAL = "!="

    # Other Operators
    TILDE = "~"
    TILDE_EQUAL = "~="
    DOLLAR = "$"
    DOLLAR_EQUAL = "$="
    CARET = "^"
    CARET_EQUAL = "^="

    # Identifiers and Literals
    IDENTIFIER = "identifier"
    INTEGER = "Int"
    DECIMAL = "Decimal"
    STRING = "String"
    BOOL = "Bool"
    ALL_TYPES = ("Int", "Decimal", "String", "Bool")


    # Advanced types: E.g lists, structs, dictionaries
    LIST = "List"
    ALL_ADVANCED_TYPES = ("List")

    # Delimiters
    COLON = ":"
    COMMA = ","
    LEFT_BRACE = "{"
    LEFT_BRACKET = "["
    LEFT_PAREN = "("
    NEWLINE = "\n"
    RIGHT_BRACE = "}"
    RIGHT_BRACKET = "]"
    RIGHT_PAREN = ")"
    SEMI_COLON = ";"

    # Special Token Types
    EOF = "EndOfFile"
    BOF = "BeginningOfFile"
    UNRECOGNIZED = "unrecognized"
    SINGLE_LINE_COMMENT = "#"
    MULTI_COMMENT_BEGIN = "/*"
    MULTI_COMMENT_END = "*/"

    # Unary Operators
    UNARY_PLUS = "Unary+"
    UNARY_MINUS = "Unary-"
