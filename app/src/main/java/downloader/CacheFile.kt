package de.binarynoise.appdate

import java.io.File

data class CacheFile(val path: String, var fileSize: Int = 0) {
	fun delete() {
		File(path).delete()
		fileSize = 0
	}
	
	fun isValid(): Boolean {
		val file = File(path)
		return file.exists() && file.isFile && file.length() == fileSize.toLong()
	}
	
	val file: File = File(path).canonicalFile
	
	override fun equals(other: Any?): Boolean {
		return (other is CacheFile) && (other.file == file)
	}
}

fun CacheFile?.isValid(): Boolean {
	return this != null && this.isValid()
}

