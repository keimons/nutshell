package com.keimons.nutshell.test;

import com.keimons.nutshell.core.NutshellLauncher;
import com.keimons.nutshell.core.monitor.IdeaHotswapObserver;
import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.Optional;
import java.util.Properties;

/**
 * 启动器
 * <p>
 * 监控src/test/resource/version.properties文件，当文件内容发生改变时，启动hotswap。
 *
 * @author houyn[monkey@keimons.com]
 * @version 1.0
 * @since 9
 */
public class Launcher implements ExecutionCondition {

	@Override
	public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
		context.getTestInstance().ifPresent(instance -> {
			try {
				NutshellLauncher.run(instance, new TestObserver());
			} catch (Throwable e) {
				e.printStackTrace();
			}
		});
		return ConditionEvaluationResult.enabled("test");
	}

	static class TestObserver extends IdeaHotswapObserver<String> {

		String path = System.getProperty("user.dir") + File.separator +
				"src" + File.separator +
				"test" + File.separator +
				"resources" + File.separator +
				"version.properties";

		@Override
		public String getNextVersion() {
			Properties properties = new Properties();
			// 使用InPutStream流读取properties文件
			try {
				BufferedReader bufferedReader = new BufferedReader(new FileReader(path));
				properties.load(bufferedReader);
				Optional<Integer> max = properties.keySet().stream().map(key -> Integer.parseInt(key.toString())).max(Integer::compareTo);
				if (max.isPresent()) {
					return max.get().toString();
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			return null;
		}
	}
}
