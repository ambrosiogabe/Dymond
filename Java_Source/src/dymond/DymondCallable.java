package dymond;

import java.util.List;

interface DymondCallable {
	Object call(Interpreter interpreter, List<Object> arguments);
	Object call(Interpreter interpreter, List<Object> arguments, Expr.Call expr);
	int arity();
}
