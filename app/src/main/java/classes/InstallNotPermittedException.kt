package de.binarynoise.appdate

class InstallNotPermittedException(message: String = "") : Exception(
	getString(R.string.install_not_permitted) + if (message.isNotBlank()) ":\n$message" else ""
)
