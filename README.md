# What is nutshell?

nutshell可以帮助你创建一个在运行时添加、更新和卸载功能的应用程序和服务。

# How does it work?

演示模块更新：

<pre>
+----------+       +----------+       +----------+       +----------+       +----------+
| Module A | ----> | Autolink | ----> | Module B | ----> | Autolink | ----> | Module C |
+----------+       +----------+       +----------+       +----------+       +----------+
</pre>

更新模块B：

<pre>
+----------+       +----------+       +----------+       +----------+       +----------+
| Module A | ----> | Autolink | ----> | Module B | ----> | Autolink | ----> | Module C |
+----------+       +----------+       +----------+       +----------+       +----------+
                                                                                 ^
                                      +----------+       +----------+            |
                                      | Module X | ----> | Autolink | -----------+
                                      +----------+       +----------+
</pre>

链接模块B：

<pre>
+----------+       +----------+       +----------+       +----------+       +----------+
| Module A | ----> | Autolink |       | Module B | ----> | Autolink | ----> | Module C |
+----------+       +----------+       +----------+       +----------+       +----------+
                        |                                                        ^
                        |             +----------+       +----------+            |
                        +-----------> | Module X | ----> | Autolink | -----------+
                                      +----------+       +----------+
</pre>

完成更新：

<pre>
+----------+       +----------+       +----------+       +----------+       +----------+
| Module A | ----> | Autolink | ----> | Module X | ----> | Autolink | ----> | Module C |
+----------+       +----------+       +----------+       +----------+       +----------+
</pre>

在模块之间插入“桥”，“桥”链接两个模块。

nutshell采用注入解耦合，按照模块划分，每个模块使用单独的类装载器装载。例如测试用例的演示：

<pre>
@ExtendWith(Launcher.class)
public class AutolinkTest {

	// 查找并注入一个ModuleASharable实现
	@Autolink
	public ModuleASharable sharable;

	@Test
	public void test() throws Throwable {
		while (true) {
			System.out.println(sharable.name());
			Thread.sleep(2000);
		}
	}
}
</pre>

<pre>
Module B
Module B
[hotswap: Y]: com.keimons.nutshell.test.link.module_a
[hotswap: N]: com.keimons.nutshell.test.link.module_b
instance class: com.keimons.nutshell.test.link.module_a.ModuleAService
inject instance: class com.keimons.nutshell.test.link.module_a.ModuleAService
class unload: interface com.keimons.nutshell.test.link.module_b.ModuleBSharable
class unload: class com.keimons.nutshell.test.link.module_a.ModuleAService
class unload: interface com.keimons.nutshell.test.link.module_a.ModuleASharable
Module A
Module B
Module A
Module B
</pre>

模块更新时，卸载旧的模块，包含Class、instance等，安装新的模块，同时“桥”指向新的模块。

# STW & SafePoint

nutshell采用引用计数，监控功能的使用情况。添加伪STW的概念，功能更新时，活动中的模块等待使用完成，
如果更新中有访问模块的需求，线程进入SafePoint之后暂停线程，等待功能更新。功能更新完成后，唤醒所有等待中的线程。