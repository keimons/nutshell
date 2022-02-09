package com.keimons.nutshell.core;

import com.keimons.nutshell.core.assembly.Assembly;
import com.keimons.nutshell.core.bootstrap.*;
import com.keimons.nutshell.core.internal.utils.RuntimeUtils;
import com.keimons.nutshell.core.monitor.ApplicationMonitor;
import com.keimons.nutshell.core.monitor.ApplicationObserver;

import java.util.List;

/**
 * App
 *
 * @author houyn[monkey@keimons.com]
 * @version 1.0
 * @since 11
 **/
public class NutshellApplication {

	public AssemblyInstaller installer;

	private ApplicationContext context;

	private ApplicationObserver<?> observer;

	private ApplicationMonitor monitor;

	private Object root;

	public NutshellApplication(Object root, ApplicationObserver<?> observer) {
		RuntimeUtils.init(root.getClass());

		installer = new AssemblyInstaller();
		context = new DefaultApplicationContext();
		monitor = new ApplicationMonitor(this, observer, root);

		installer.addLast("update", new UpdateBootstrap());
		installer.addLast("init", new InitBootstrap());
		installer.addLast("inject", new InjectBootstrap());
		installer.addLast("autolink", new AutolinkBootstrap());

		monitor.monitor();
	}

	public NutshellApplication(AssemblyInstaller installer, ApplicationContext context) {
		this.installer = installer;
		this.context = context;
	}

	public void install(List<Assembly> assemblies) throws Throwable {
		installer.install(context, assemblies);
	}

	public void hotswap(String packageName) throws Throwable {
		Assembly assembly = context.get(packageName);
		installer.hotswap(context, assembly);
	}

	public void hotswap(List<Assembly> assemblies) throws Throwable {
		installer.hotswap(context, assemblies);
	}

	public ApplicationContext getContext() {
		return context;
	}
}
