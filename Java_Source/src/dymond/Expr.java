package dymond;

import java.util.List;
public abstract class Expr{
	public interface Visitor <R> {
		R visitAssignExpr(Assign expr);
		R visitBinaryExpr(Binary expr);
		R visitCallExpr(Call expr);
		R visitTernaryExpr(Ternary expr);
		R visitGroupingExpr(Grouping expr);
		R visitLiteralExpr(Literal expr);
		R visitLogicalExpr(Logical expr);
		R visitUnaryExpr(Unary expr);
		R visitUnaryAssignExpr(UnaryAssign expr);
		R visitVariableExpr(Variable expr);
	}

	abstract <R> R accept(Visitor<R> visitor);

	public static class Assign extends Expr{
		public Assign(Token name, Expr value, Token operator) {
			this.name = name;
			this.value = value;
			this.operator = operator;
		}

		public <R> R accept(Visitor<R> visitor) {
			return visitor.visitAssignExpr(this);
		}


		public final Token name;
		public final Expr value;
		public final Token operator;
	}

	public static class Binary extends Expr{
		public Binary(Expr left, Token operator, Expr right) {
			this.left = left;
			this.operator = operator;
			this.right = right;
		}

		public <R> R accept(Visitor<R> visitor) {
			return visitor.visitBinaryExpr(this);
		}


		public final Expr left;
		public final Token operator;
		public final Expr right;
	}

	public static class Call extends Expr{
		public Call(Expr callee, Token paren, List<Expr> arguments) {
			this.callee = callee;
			this.paren = paren;
			this.arguments = arguments;
		}

		public <R> R accept(Visitor<R> visitor) {
			return visitor.visitCallExpr(this);
		}


		public final Expr callee;
		public final Token paren;
		public final List<Expr> arguments;
	}

	public static class Ternary extends Expr{
		public Ternary(Expr left, Expr right, Expr condition) {
			this.left = left;
			this.right = right;
			this.condition = condition;
		}

		public <R> R accept(Visitor<R> visitor) {
			return visitor.visitTernaryExpr(this);
		}


		public final Expr left;
		public final Expr right;
		public final Expr condition;
	}

	public static class Grouping extends Expr{
		public Grouping(Expr expression) {
			this.expression = expression;
		}

		public <R> R accept(Visitor<R> visitor) {
			return visitor.visitGroupingExpr(this);
		}


		public final Expr expression;
	}

	public static class Literal extends Expr{
		public Literal(Object value) {
			this.value = value;
		}

		public <R> R accept(Visitor<R> visitor) {
			return visitor.visitLiteralExpr(this);
		}


		public final Object value;
	}

	public static class Logical extends Expr{
		public Logical(Expr left, Token operator, Expr right) {
			this.left = left;
			this.operator = operator;
			this.right = right;
		}

		public <R> R accept(Visitor<R> visitor) {
			return visitor.visitLogicalExpr(this);
		}


		public final Expr left;
		public final Token operator;
		public final Expr right;
	}

	public static class Unary extends Expr{
		public Unary(Token operator, Expr right) {
			this.operator = operator;
			this.right = right;
		}

		public <R> R accept(Visitor<R> visitor) {
			return visitor.visitUnaryExpr(this);
		}


		public final Token operator;
		public final Expr right;
	}

	public static class UnaryAssign extends Expr{
		public UnaryAssign(Token name, Token operator) {
			this.name = name;
			this.operator = operator;
		}

		public <R> R accept(Visitor<R> visitor) {
			return visitor.visitUnaryAssignExpr(this);
		}


		public final Token name;
		public final Token operator;
	}

	public static class Variable extends Expr{
		public Variable(Token name) {
			this.name = name;
		}

		public <R> R accept(Visitor<R> visitor) {
			return visitor.visitVariableExpr(this);
		}


		public final Token name;
	}
}