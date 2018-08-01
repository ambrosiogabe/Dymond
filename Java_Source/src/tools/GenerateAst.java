package tools;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;

public class GenerateAst {
	public static void main(String[] args) throws IOException {
		if(args.length != 1) {
			System.err.print("Usage: generate_ast <output_dir>");
			System.exit(1);
		}
		String outputDir = args[0];
		defineAst(outputDir, "Expr", Arrays.asList(
				"Assign   : Token name, Expr value, Token operator",
				"Binary   : Expr left, Token operator, Expr right",
				"Ternary  : Expr left, Expr right, Expr condition",
				"Grouping : Expr expression",
				"Literal  : Object value",
				"Unary    : Token operator, Expr right",
				"Variable : Token name"
		));
		
		defineAst(outputDir, "Stmt", Arrays.asList(
				"Block      : List<Stmt> statements",
				"Expression : Expr expression",
				"Print      : Expr expression",
				"Var        : Token name, Expr initializer"
		));
	}
	
	private static void defineAst(String outputDir, String baseName, List<String> types) throws IOException {
		String path = outputDir + "/" + baseName + ".java";
		PrintWriter writer = new PrintWriter(path, "UTF-8");
		
		writer.println("package dymond;");
		writer.println("");
		writer.println("import java.util.List;");
		writer.println("public abstract class " + baseName + "{");
		
		defineVisitor(writer, baseName, types);
		
		for(String type : types) {
			String className = type.split(":")[0].trim();
			String fields = type.split(":")[1].trim();
			defineType(writer, baseName, className, fields);
		}
		
		writer.println("}");
		writer.close();
	}
	
	private static void defineVisitor(PrintWriter writer, String baseName, List<String> types) {
		writer.println("	public interface Visitor <R> {");
		
		for(String type : types) {
			String typeName = type.split(":")[0].trim();
			writer.println("		R visit" + typeName + baseName + "(" + typeName + " "
					+ baseName.toLowerCase() + ");");
		}
		writer.println("	}");
		writer.println();
		
		writer.println("	abstract <R> R accept(Visitor<R> visitor);");
		
	}
	
	private static void defineType(PrintWriter writer, String baseName, String className, String fieldList) {
		writer.println();
		writer.println("	public static class " + className + " extends " + baseName + "{");
		
		// Constructor
		writer.println("		public " + className + "(" + fieldList + ") {");
		
		String[] fields = fieldList.split(", ");
		for(String field : fields) {
			String name = field.split(" ")[1];
			writer.println("			this." + name + " = " + name + ";");
		}
		
		writer.println("		}");
		
		writer.println();
		writer.println("		public <R> R accept(Visitor<R> visitor) {");
		writer.println("			return visitor.visit" + className + baseName + "(this);");
		writer.println("		}");
		writer.println();
		
		// Fields
		writer.println();
		for(String field : fields) {
			writer.println("		public final " + field + ";");
		}
		
		writer.println("	}");
	}
}
