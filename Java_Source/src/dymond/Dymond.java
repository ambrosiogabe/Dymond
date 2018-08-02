package dymond;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.TimeUnit;

import dymond.Parser;

public class Dymond {
	
	private static final Interpreter interpreter = new Interpreter();
	public static boolean hadError = false;
	public static boolean hadRuntimeError = false;

	public static void main(String[] args) throws IOException, InterruptedException {		
		if (args.length > 1) {
			System.out.println("Usage: dymond [script]");
			System.exit(64);
		} else if (args.length == 1) {
			//runPrompt(); // Uncomment to run prompt
			runFile(args[0]);
		} else {
			runPrompt();
		}
	}
	
	private static void runFile(String path) throws IOException {
		byte[] bytes = Files.readAllBytes(Paths.get(path));
		run(new String(bytes, Charset.defaultCharset()), false);
		if (hadError) System.exit(65);
		if (hadRuntimeError) System.exit(70);;
	}
	
	private static void runPrompt() throws IOException, InterruptedException {
		InputStreamReader input = new InputStreamReader(System.in);
		BufferedReader reader = new BufferedReader(input);
		
		for(;;) {
			System.out.print(">>> ");
			run(reader.readLine(), true);
			if(hadError || hadRuntimeError) {
				TimeUnit.SECONDS.sleep((long) .4);
			}
			
			hadError = false;
			hadRuntimeError = false;
		}
	}
	
	private static void run(String source, boolean repl) {
		Scanner scanner = new Scanner(source);
		List<Token> tokens = scanner.scanTokens();
		Parser parser = new Parser(tokens, repl);
		List<Stmt> statements = parser.parse();
		
		if (hadError) return;
		
		Resolver resolver = new Resolver(interpreter);
		resolver.resolve(statements);
		
		if (hadError) return;
		
		interpreter.interpret(statements, repl);
	}
	
	public static void error(int line, String message, String lineText, int column) {
		report(line, "", message, lineText, column);
	}
	
	public static void runtimeError(RuntimeError error) {
		String message = error.getMessage();
		String where = "";
		String lineText = error.token.lineText;
		int line = error.token.line;
		int column = error.token.column;
		
		String errorText = "[line " + line + "] Error " + where + ": " + message + "\n";
		errorText += "    	" + line + ".| " + lineText + "\n";
		errorText += "    	";
		if(line < 10) {
			errorText += "    ";
		} else if(line < 100) {
			errorText += "     ";
		} else if(line < 1000) {
			errorText += "      ";
		} else {
			errorText += "       ";
		}
		
		for(int i=0; i < column; i++) {
			errorText += " ";
		}
		errorText += "^-- Here.";
		
		
		System.err.println(errorText);
		hadRuntimeError = true;
	}
	
	
	private static void report(int line, String where, String message, String lineText, int column) {
		String error = "[line " + line + "] Error " + where + ": " + message + "\n";
		error += "    " + line + ".| " + lineText + "\n";
		error += "    ";
		if(line < 10) {
			error += "    ";
		} else if(line < 100) {
			error += "     ";
		} else if(line < 1000) {
			error += "      ";
		} else {
			error += "       ";
		}
		
		for(int i=0; i < column; i++) {
			error += " ";
		}
		error += "^-- Here.";
		
		System.err.println(error);
		hadError = true;
	}
}
