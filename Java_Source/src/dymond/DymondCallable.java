package dymond;

import java.util.List;

interface DymondCallable {
	Object call(Interpreter interpreter, List<Object> arguments);
	int arity();
}
