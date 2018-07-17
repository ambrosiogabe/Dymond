class Token:
    type = None
    value = None
    line = -1
    column = -1

    def __init__(self, type, value, line, column):
        self.type = type
        self.value = value
        self.line = line
        self.column = column

    def length(self):
        return len(self.type)

    def get_value(self):
        return self.value

    def get_type(self):
        return self.type

    def get_line(self):
        return self.line

    def to_string(self):
        return "<" + self.type + ", " + self.value + ", " + self.line \
        + ":" + self.column + ">"
