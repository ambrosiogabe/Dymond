class TokenType:
    # Keywords
    ABSTRACT = "abstract"
    AS = "as"
    CLASS = "class"
    ELSE = "else"
    EXTENDS = "extends"
    FALSE = "false"
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
    TRUE = "true"
    VAR = "var"
    WHILE = "while"

    # Dispatch Operators
    DOT = "."

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
    INTEGER = "integer"
    DECIMAL = "decimal"
    STRING = "string"

    # Types
    INTEGER_TYPE = "Int"
    DECIMAL_TYPE = "Decimal"
    STRING_TYPE = "String"
    ALL_TYPES = ("Int", "Decimal", "String")

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
