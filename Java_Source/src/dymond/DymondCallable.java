package dymond;

import java.util.List;

interface DymondCallable {
	Object call(Interpreter interpreter, List<Expr> arguments, Expr.Call expr);
	Object call1(Interpreter interpreter, List<Object> arguments, Expr.Call expr);
	int minArity();
	int maxArity();
}
