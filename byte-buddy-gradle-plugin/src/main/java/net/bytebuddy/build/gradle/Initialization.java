package net.bytebuddy.build.gradle;

import net.bytebuddy.build.EntryPoint;

/**
 * Defines a configuration for a Maven build's type transformation.
 */
public class Initialization {

	private String entryPoint;

	public Initialization() {
	}

	public static Initialization makeDefault() {
		Initialization initialization = new Initialization();
		initialization.setEntryPoint(EntryPoint.Default.REBASE.name());
		return initialization;
	}

	public String getEntryPoint() {
		return entryPoint;
	}

	public void setEntryPoint(String entryPoint) {
		this.entryPoint = entryPoint;
	}

	public EntryPoint toEntryPoint(ClassLoaderResolver classLoaderResolver) {
		return EntryPoint.Default.valueOf(entryPoint);
	}
}
