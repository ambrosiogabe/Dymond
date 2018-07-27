package lexer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class Dymond {
	
	private static boolean hadError = false;

	public static void main(String[] args) throws IOException {		
		if (args.length > 1) {
			System.out.println("Usage: dymond [script]");
			System.exit(64);
		} else if (args.length == 1) {
			runFile(args[0]);
		} else {
			runPrompt();
		}
	}
	
	private static void runFile(String path) throws IOException {
		byte[] bytes = Files.readAllBytes(Paths.get(path));
		run(new String(bytes, Charset.defaultCharset()));
		if(hadError) System.exit(65);
	}
	
	private static void runPrompt() throws IOException {
		InputStreamReader input = new InputStreamReader(System.in);
		BufferedReader reader = new BufferedReader(input);
		
		for(;;) {
			System.out.print(">>> ");
			run(reader.readLine());
			hadError = false;
		}
	}
	
	private static void run(String source) {
		Scanner scanner = new Scanner(source);
		List<Token> tokens = scanner.scanTokens();
		
		for (Token token : tokens) {
			System.out.println(tokens);
		}
	}
	
	public static void error(int line, String message, String lineText, int column) {
		report(line, "", message, lineText, column);
	}
	
	
	private static void report(int line, String where, String message, String lineText, int column) {
		String error = "[line " + line + "] Error " + where + ": " + message + "\n";
		error += "    " + lineText + "\n";
		error += "    ";
		for(int i=0; i < column; i++) {
			error += " ";
		}
		error += "^-- Here.";
		
		System.err.println(error);
		hadError = true;
	}
}
