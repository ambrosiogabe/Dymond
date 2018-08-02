package dymond;

public class BreakError extends RuntimeException {
	public final Token token;
	public BreakError(Token token) {
		super(null, null, false, false);
		this.token = token;
	}
}
