package com.keimons.nutshell.core;

import com.keimons.nutshell.core.assembly.Assembly;
import com.keimons.nutshell.core.bootstrap.*;

import java.util.List;

/**
 * App
 *
 * @author houyn[monkey@keimons.com]
 * @version 1.0
 * @since 9
 **/
public class NutshellApplication {

	public AssemblyInstaller installer;

	private ApplicationContext context;

	public NutshellApplication() {
		installer = new AssemblyInstaller();
		context = new DefaultApplicationContext();

		installer.addLast("update", new UpdateBootstrap());
		installer.addLast("init", new InitBootstrap());
		installer.addLast("inject", new InjectBootstrap());
		installer.addLast("autolink", new AutolinkBootstrap());
	}

	public NutshellApplication(AssemblyInstaller installer, ApplicationContext context) {
		this.installer = installer;
		this.context = context;
	}

	public void setup(List<Assembly> assemblies) throws Throwable {
		installer.install(context, assemblies);
	}

	public void update(String packageName) throws Throwable {
		Assembly assembly = context.get(packageName);
		installer.update(context, assembly);
	}

	public void update(List<Assembly> assemblies) throws Throwable {
		installer.update(context, assemblies);
	}

	public ApplicationContext getContext() {
		return context;
	}
}
