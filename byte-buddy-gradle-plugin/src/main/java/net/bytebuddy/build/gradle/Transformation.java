package net.bytebuddy.build.gradle;

import java.io.File;

public class Transformation {
	private String plugin;

	private Iterable<File> classpath;

	public Transformation() {

	}

	public String getPlugin() {
		return plugin;
	}

	public void setPlugin(String plugin) {
		this.plugin = plugin;
	}

	public Iterable<File> getClasspath() {
		return classpath;
	}

	public void setClasspath(Iterable<File> classpath) {
		this.classpath = classpath;
	}
}
