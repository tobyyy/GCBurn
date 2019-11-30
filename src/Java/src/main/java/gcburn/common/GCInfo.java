package gcburn.common;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryManagerMXBean;
import java.util.List;

//todo
public class GCInfo {

	private static void foo() {
		List<GarbageCollectorMXBean> garbageCollectorMXBeans = ManagementFactory.getGarbageCollectorMXBeans();

		List<MemoryManagerMXBean> memoryManagerMXBeans = ManagementFactory.getMemoryManagerMXBeans();

		for (MemoryManagerMXBean bean : memoryManagerMXBeans) {
//			bean.
		}

		for (GarbageCollectorMXBean bean : garbageCollectorMXBeans) {
//			bean.
		}
	}
}
