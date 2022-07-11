package com.keimons.nutshell.test.safepoint;

import com.keimons.nutshell.core.Autolink;
import com.keimons.nutshell.core.NutshellApplication;
import com.keimons.nutshell.core.NutshellLauncher;
import com.keimons.nutshell.core.internal.utils.PackageUtils;
import com.keimons.nutshell.core.internal.utils.ThrowableUtils;
import com.keimons.nutshell.test.Launcher;
import com.keimons.nutshell.test.safepoint.module_a.ModuleASharable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Set;

/**
 * 注入自动链接hotswap测试
 *
 * <pre>
 * +------+       +----------+       +----------+       +----------+       +----------+
 * | root | ----> | Autolink | ----> | Module A | ----> | Autolink | ----> | Module B |
 * +------+       +----------+       +----------+       +----------+       +----------+
 * </pre>
 * 测试更新ModuleA和ModuleB模块，添加/移除方法和字段。
 *
 * @author houyn[monkey@keimons.com]
 * @version 1.0
 * @since 17
 **/
@ExtendWith(Launcher.class)
public class SafePointTest {

	// 查找并注入一个ModuleASharable实现
	@Autolink
	public ModuleASharable sharable;

	@Test
	public void test() throws Throwable {
		for (int i = 0; i < 10; i++) {
			int index = i;
			Thread thread = new Thread(ThrowableUtils.wrapper(() -> {
				// thread 0, 2, 4, 6, 8 wait 1000ms start.
				if ((index & 1) == 0) {
					Thread.sleep(1000);
				}
				System.out.println(sharable.name());
			}), "Thread-" + i);
			thread.start();
		}
		NutshellApplication application = NutshellLauncher.getApplication();
		String packageName = this.getClass().getPackageName();
		Set<String> subpackages = PackageUtils.findSubpackages(packageName);

		Thread.sleep(500);
		application.hotswap(subpackages.toArray(new String[0]));
		System.gc();

		Thread.sleep(5000);
	}
}
