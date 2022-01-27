package com.keimons.nutshell.test;

import com.keimons.nutshell.core.App;
import com.keimons.nutshell.core.assembly.Assembly;
import org.junit.jupiter.api.extension.*;

import java.util.Optional;

/**
 * 启动器
 *
 * @author houyn[monkey@keimons.com]
 * @version 1.0
 * @since 9
 **/
public class Launcher implements TestInstanceFactory, ExecutionCondition {

	@Override
	public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
		context.getTestInstance().ifPresent(instance -> {
			try {
				Assembly root = Assembly.root(instance);

				String[] classNames1 = new String[]{
						"com.keimons.nutshell.test.link.module1.Module1Service",
						"com.keimons.nutshell.test.link.module1.Module1Sharable"
				};
				Assembly module1 = Assembly.of("module1", classNames1);

				String[] classNames2 = new String[]{
						"com.keimons.nutshell.test.link.module2.Module2Service",
						"com.keimons.nutshell.test.link.module2.Module2Sharable"
				};
				Assembly module2 = Assembly.of("module2", classNames2);

				App.getInstance().context.init(root);
				App.getInstance().context.init(module1);
				App.getInstance().context.init(module2);
				App.getInstance().context.install(root);
				App.getInstance().context.install(module1);
				App.getInstance().context.install(module2);
				App.getInstance().context.link(root);
				App.getInstance().context.link(module1);
				App.getInstance().context.link(module2);
			} catch (Throwable e) {
				e.printStackTrace();
			}
		});
		return ConditionEvaluationResult.enabled("test");
	}

	@Override
	public Object createTestInstance(TestInstanceFactoryContext factoryContext, ExtensionContext extensionContext) throws TestInstantiationException {
		Optional<Class<?>> optional = extensionContext.getTestClass();
		if (optional.isPresent()) {
			try {
				return optional.get().getConstructor().newInstance();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return extensionContext.getTestInstance();
	}
}
