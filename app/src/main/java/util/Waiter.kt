package de.binarynoise.appdate

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*

class Waiter {
	private val channel: Channel<Unit> = Channel()
	var timeout: Long = 20_000
	
	@Throws(TimeoutCancellationException::class)
	fun sleep() = runBlocking { withTimeout(timeout) { channel.receive() } }
	
	fun wake() = channel.offer(Unit)
}

fun Throwable.throwIt() {
	throw this
}
