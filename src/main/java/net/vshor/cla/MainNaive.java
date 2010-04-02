package net.vshor.cla;

import java.io.File;
import java.io.PrintStream;

public class MainNaive {

	public static void main(String[] args) throws Exception {
		if (args.length < 1) {
			System.out.println("No arguments supplied");
		}

		ClassloaderLeakDetector cleak = new ClassloaderLeakDetector(new File(args[0]));
		PrintStream out = System.out;
		if (args.length == 2) {
			out = new PrintStream(args[1]);
		}
		cleak.outputResultsAsXml(out);
	}

}