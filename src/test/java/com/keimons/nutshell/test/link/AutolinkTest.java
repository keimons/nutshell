package com.keimons.nutshell.test.link;

import com.keimons.nutshell.core.Autolink;
import com.keimons.nutshell.core.NutshellApplication;
import com.keimons.nutshell.core.NutshellLauncher;
import com.keimons.nutshell.core.internal.utils.PackageUtils;
import com.keimons.nutshell.test.Launcher;
import com.keimons.nutshell.test.link.module_a.ModuleASharable;
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
public class AutolinkTest {

	// 查找并注入一个ModuleASharable实现
	@Autolink
	public ModuleASharable sharable;

	@Test
	public void test() throws Throwable {
//		while (true) {
		for (int i = 0; i < 10; i++) {
			int index = i;
			Thread thread = new Thread(() -> {
				if ((index & 1) == 0) {
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
				System.out.println(sharable.name());
			}, "Thread-" + i);
			thread.start();
		}
		NutshellApplication application = NutshellLauncher.getApplication();
		String packageName = this.getClass().getPackageName();
		Set<String> subpackages = PackageUtils.findSubpackages(packageName);
		try {
			Thread.sleep(500);
			application.hotswap(subpackages.toArray(new String[0]));
			System.gc();
		} catch (Throwable e) {
			e.printStackTrace();
		}
		System.out.println(sharable.name());

		Thread.sleep(5000);
//			Thread.sleep(2000);
//		}
	}
}
