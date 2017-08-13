package com.finoramic.ppm;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;

public class PackageManager {
	
	// TODO Fix process exit problem. Reason: unknown

	private final File buildDir;
	private volatile List<String> failures;

	public PackageManager() {
		buildDir = new File(".pp");
		if (!buildDir.exists())
			buildDir.mkdir();

		failures = new ArrayList<>();
	}

	/**
	 * Create virtual environment
	 * @throws IOException
	 */
	public void init() throws IOException {
		Process init = Runtime.getRuntime().exec("python -m venv " + buildDir.getAbsolutePath());

		try {
			init.waitFor();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		if (init.exitValue() != 0) {
			String message = getResponse(init.getErrorStream());
			throw new IOException(message);
		}
	}

	/**
	 * Get List of dependencies from package.json file
	 * @return list of dependencies
	 * @throws IOException
	 */
	public List<String> getDependencies() throws IOException {
		PackageJSON packageJSON = new ObjectMapper()
				.readValue(this.getClass().getClassLoader().getResourceAsStream("package.json"), PackageJSON.class);
		return packageJSON.getDependencies();
	}

	/**
	 * Start package manager
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public void start() throws IOException, InterruptedException {
		init();
		List<String> dependencies = getDependencies();

		System.out.println(dependencies);
		ExecutorService pool = Executors.newFixedThreadPool(5);
		for (String dependency : dependencies) {
			pool.submit(() -> {
				install(dependency);
			});
		}

		pool.shutdown();
		pool.awaitTermination(2, TimeUnit.MINUTES);

		if (failures.size() == 0) {
			System.out.println("Success");
		} else {
			System.out.println("Failed:");
			failures.forEach(System.out::println);
		}
	}

	/**
	 * Install process
	 * @param dependency the dependency to install
	 */
	public void install(String dependency) {
		BufferedWriter writer = null;
		try {
			Process install = Runtime.getRuntime().exec(App.getSourceCommand(), null, buildDir);
			writer = new BufferedWriter(new OutputStreamWriter(install.getOutputStream()));

			writer.write("pip install " + dependency);
			writer.newLine();
			writer.flush();

			writer.write("exit");
			writer.newLine();
			writer.flush();

			install.waitFor();
			if (install.exitValue() == 0) {
				System.out.println(getResponse(install.getInputStream()));
			} else {
				failures.add(dependency);
				System.out.println(getResponse(install.getErrorStream()));
			}
		} catch (IOException | InterruptedException e) {
			e.printStackTrace();
		} finally {
			try {
				if (writer != null)
					writer.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Get process response, by parsing input streams
	 * @param input the input stream
	 * @return response as String
	 * @throws IOException
	 */
	private String getResponse(InputStream input) throws IOException {
		try (BufferedReader buffer = new BufferedReader(new InputStreamReader(input))) {
			return buffer.lines().collect(Collectors.joining("\n"));
		}
	}
}
