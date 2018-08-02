package dymond;

public class Next extends RuntimeException {
	public final Token token;
	public Next(Token token) {
		super(null, null, false, false);
		this.token = token;
	}
}
