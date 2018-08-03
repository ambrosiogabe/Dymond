package dymond;

import java.util.List;
public abstract class Stmt{
	public interface Visitor <R> {
		R visitBlockStmt(Block stmt);
		R visitBreakStmt(Break stmt);
		R visitClassStmt(Class stmt);
		R visitExpressionStmt(Expression stmt);
		R visitFunctionStmt(Function stmt);
		R visitIfStmt(If stmt);
		R visitNextStmt(Next stmt);
		R visitReturnStmt(Return stmt);
		R visitVarStmt(Var stmt);
		R visitWhileStmt(While stmt);
		R visitForStmt(For stmt);
	}

	abstract <R> R accept(Visitor<R> visitor);

	public static class Block extends Stmt{
		public Block(List<Stmt> statements) {
			this.statements = statements;
		}

		public <R> R accept(Visitor<R> visitor) {
			return visitor.visitBlockStmt(this);
		}


		public final List<Stmt> statements;
	}

	public static class Break extends Stmt{
		public Break(Token keyword) {
			this.keyword = keyword;
		}

		public <R> R accept(Visitor<R> visitor) {
			return visitor.visitBreakStmt(this);
		}


		public final Token keyword;
	}

	public static class Class extends Stmt{
		public Class(Token name, List<Stmt.Function> methods, List<Stmt.Function> staticMethods, Expr.Variable superclass) {
			this.name = name;
			this.methods = methods;
			this.staticMethods = staticMethods;
			this.superclass = superclass;
		}

		public <R> R accept(Visitor<R> visitor) {
			return visitor.visitClassStmt(this);
		}


		public final Token name;
		public final List<Stmt.Function> methods;
		public final List<Stmt.Function> staticMethods;
		public final Expr.Variable superclass;
	}

	public static class Expression extends Stmt{
		public Expression(Expr expression) {
			this.expression = expression;
		}

		public <R> R accept(Visitor<R> visitor) {
			return visitor.visitExpressionStmt(this);
		}


		public final Expr expression;
	}

	public static class Function extends Stmt{
		public Function(Token name, List<Expr.Assign> parameters, List<Stmt> body) {
			this.name = name;
			this.parameters = parameters;
			this.body = body;
		}

		public <R> R accept(Visitor<R> visitor) {
			return visitor.visitFunctionStmt(this);
		}


		public final Token name;
		public final List<Expr.Assign> parameters;
		public final List<Stmt> body;
	}

	public static class If extends Stmt{
		public If(Expr condition, Stmt thenBranch, Stmt elseBranch) {
			this.condition = condition;
			this.thenBranch = thenBranch;
			this.elseBranch = elseBranch;
		}

		public <R> R accept(Visitor<R> visitor) {
			return visitor.visitIfStmt(this);
		}


		public final Expr condition;
		public final Stmt thenBranch;
		public final Stmt elseBranch;
	}

	public static class Next extends Stmt{
		public Next(Token keyword) {
			this.keyword = keyword;
		}

		public <R> R accept(Visitor<R> visitor) {
			return visitor.visitNextStmt(this);
		}


		public final Token keyword;
	}

	public static class Return extends Stmt{
		public Return(Token keyword, Expr value) {
			this.keyword = keyword;
			this.value = value;
		}

		public <R> R accept(Visitor<R> visitor) {
			return visitor.visitReturnStmt(this);
		}


		public final Token keyword;
		public final Expr value;
	}

	public static class Var extends Stmt{
		public Var(Token name, Expr initializer) {
			this.name = name;
			this.initializer = initializer;
		}

		public <R> R accept(Visitor<R> visitor) {
			return visitor.visitVarStmt(this);
		}


		public final Token name;
		public final Expr initializer;
	}

	public static class While extends Stmt{
		public While(Expr condition, Stmt body) {
			this.condition = condition;
			this.body = body;
		}

		public <R> R accept(Visitor<R> visitor) {
			return visitor.visitWhileStmt(this);
		}


		public final Expr condition;
		public final Stmt body;
	}

	public static class For extends Stmt{
		public For(Expr condition, Stmt body, Stmt increment) {
			this.condition = condition;
			this.body = body;
			this.increment = increment;
		}

		public <R> R accept(Visitor<R> visitor) {
			return visitor.visitForStmt(this);
		}


		public final Expr condition;
		public final Stmt body;
		public final Stmt increment;
	}
}
