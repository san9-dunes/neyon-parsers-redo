package org.koitharu.kotatsu.parsers

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.jupiter.api.Test
import org.koitharu.kotatsu.parsers.model.MangaListFilter
import org.koitharu.kotatsu.parsers.model.MangaParserSource
import java.io.File

internal class SourceBatchHealthCheckTest {

	private val context = MangaLoaderContextMock

	@Test
	fun checkBatch() = runBlocking {
		val batchSize =
			(System.getProperty("batch.size") ?: System.getenv("BATCH_SIZE"))?.toIntOrNull()
				?: 100
		val batchIndex =
			(System.getProperty("batch.index") ?: System.getenv("BATCH_INDEX"))?.toIntOrNull()
				?: 0
		val batchStart =
			(System.getProperty("batch.start") ?: System.getenv("BATCH_START"))?.toIntOrNull()
				?: batchIndex
		val batchEnd =
			(System.getProperty("batch.end") ?: System.getenv("BATCH_END"))?.toIntOrNull()
				?: batchIndex
		val timeoutMs =
			(System.getProperty("source.timeout.ms") ?: System.getenv("SOURCE_TIMEOUT_MS"))?.toLongOrNull()
				?: 60_000L

		val sources = MangaParserSource.entries.filterNot { it.isBroken }

		for (currentBatch in batchStart..batchEnd) {
			val from = (currentBatch * batchSize).coerceAtMost(sources.size)
			val to = (from + batchSize).coerceAtMost(sources.size)
			if (from >= to) {
				println("BATCH_SKIP,index=$currentBatch,reason=out_of_range")
				continue
			}
			val subset = sources.subList(from, to)

			val outDir = File("build/reports/source-health")
			outDir.mkdirs()
			val reportFile = File(outDir, "batch-${currentBatch.toString().padStart(2, '0')}.csv")
			reportFile.bufferedWriter().use { writer ->
				writer.appendLine("source,status,count,error")
				println("BATCH_START,index=$currentBatch,range=$from..${to - 1},size=${subset.size}")

				subset.forEachIndexed { idx, source ->
					val row = runCatching {
						val parser = context.newParserInstance(source)
						val order = parser.availableSortOrders.first()
						val list = withTimeout(timeoutMs) {
							parser.getList(0, order, MangaListFilter())
						}
						if (list.isEmpty()) {
							"${source.name},EMPTY,0,"
						} else {
							"${source.name},OK,${list.size},"
						}
					}.getOrElse { e ->
						val cloudFlareCause = e.findCauseBySimpleName("CloudFlareProtectedException")
						val actualError = cloudFlareCause ?: e.rootCause()
						val status = if (cloudFlareCause != null) "BLOCKED" else "ERROR"
						val msg = (actualError.message ?: actualError::class.simpleName ?: "unknown")
							.replace('"', '\'')
							.replace('\n', ' ')
							.take(300)
						"${source.name},$status,0,${actualError::class.simpleName}: $msg"
					}

					writer.appendLine(row)
					println("BATCH_ITEM,index=$currentBatch,item=${idx + 1}/${subset.size},$row")
				}
			}
		}
	}

	private fun Throwable.rootCause(): Throwable {
		var current = this
		while (current.cause != null && current.cause !== current) {
			current = current.cause!!
		}
		return current
	}

	private fun Throwable.findCauseBySimpleName(simpleName: String): Throwable? {
		var current: Throwable? = this
		while (current != null) {
			if (current::class.simpleName == simpleName) {
				return current
			}
			current = current.cause
		}
		return null
	}
}
