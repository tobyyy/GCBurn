package gcburn.common

import spock.lang.Specification

class HardwareInfoSpec extends Specification {
	def "getCpuModelName has output"() {
		when:
		def cpu = HardwareInfo.getCpuModelName()
		println("Platform cpu: " + cpu)

		then:
		cpu.length() > 0
	}

	def "getRamSize has output"() {
		when:
		def ram = HardwareInfo.getRamSize()
		println("Platform ram size: " + ram + "GB")

		then:
		ram > 0
	}
}