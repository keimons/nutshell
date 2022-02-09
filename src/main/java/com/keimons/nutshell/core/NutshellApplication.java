package com.keimons.nutshell.core;

import com.keimons.nutshell.core.assembly.Assembly;
import com.keimons.nutshell.core.bootstrap.*;
import com.keimons.nutshell.core.internal.utils.RuntimeUtils;
import com.keimons.nutshell.core.monitor.HotswapMonitor;
import com.keimons.nutshell.core.monitor.HotswapObserver;

import java.util.ArrayList;
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

	private HotswapObserver<?> observer;

	private HotswapMonitor monitor;

	private Object root;

	public NutshellApplication(Object root, HotswapObserver<?> observer) {
		RuntimeUtils.init(root.getClass());

		installer = new AssemblyInstaller();
		context = new DefaultApplicationContext();
		monitor = new HotswapMonitor(this, observer, root, 1000);

		installer.addLast("update", new UpdateBootstrap());
		installer.addLast("init", new InitBootstrap());
		installer.addLast("inject", new InjectBootstrap());
		installer.addLast("autolink", new AutolinkBootstrap());

		monitor.start();
	}

	public NutshellApplication(AssemblyInstaller installer, ApplicationContext context) {
		this.installer = installer;
		this.context = context;
	}

	public void install(List<Assembly> assemblies) throws Throwable {
		installer.install(context, assemblies);
	}

	public void hotswap(String... packages) throws Throwable {
		List<Assembly> assemblies = new ArrayList<>(packages.length);
		for (String pkg : packages) {
			assemblies.add(context.get(pkg));
		}
		installer.hotswap(context, assemblies);
	}

	public void hotswap(List<Assembly> assemblies) throws Throwable {
		installer.hotswap(context, assemblies);
	}

	public ApplicationContext getContext() {
		return context;
	}
}
