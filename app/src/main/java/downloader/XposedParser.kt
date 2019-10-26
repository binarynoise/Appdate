package de.binarynoise.appdate

import com.vdurmont.semver4j.Semver
import java.io.IOException

object XposedParser {
	
	@Throws(IOException::class)
	fun parse(app: App): Tupel<Semver, String> {
		app.useVersionNumberInsteadOfName = true
		return GenericParser.parse(app)
	}
}
