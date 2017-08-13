package com.finoramic.ppm;

import java.io.IOException;

public class App {
	
	private static final String sourceCommand;
	
	static {
		if (isWindows()) {
			sourceCommand = "cmd /K call Scripts/activate.bat";
		} else {
			sourceCommand =  "/bin/bash source Scripts/activate";
		}
	}
	
	public static boolean isWindows() {
		return System.getProperty("os.name").contains("Windows");
	}
	
	public static String getSourceCommand() {
		return sourceCommand;
	}
	
	public static void main(String[] args) throws IOException, InterruptedException {
		new PackageManager().start();
	}
}
