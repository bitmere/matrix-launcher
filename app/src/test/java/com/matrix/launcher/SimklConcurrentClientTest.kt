package com.matrix.launcher

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import java.nio.file.Files
import java.util.concurrent.Callable
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

class SimklConcurrentClientTest {
    @Test fun concurrentStatusesShareOneClientSnapshotAndActivitiesRequest() {
        val dir = Files.createTempDirectory("simkl-client").toFile()
        val pool = Executors.newFixedThreadPool(2)
        val counts = ConcurrentHashMap<String, AtomicInteger>()
        val start = CountDownLatch(1)
        val client = SimklClient(mapBackedPrefs(), mapBackedPrefs(), dir) { path, _, _ ->
            counts.computeIfAbsent(path) { AtomicInteger() }.incrementAndGet()
            when (path) {
                "/sync/activities" -> JSONObject("""{"all":"stamp","tv_shows":{"all":"stamp"}}""")
                "/sync/all-items/shows" -> JSONObject("""{"shows":[
                    {"status":"watching","show":{"title":"Watching","ids":{"simkl":1,"imdb":"tt0000001"}}},
                    {"status":"plantowatch","show":{"title":"Planned","ids":{"simkl":2,"imdb":"tt0000002"}}}
                ]}""")
                else -> error("Unexpected request: $path")
            }
        }
        try {
            val futures = listOf("watching", "plantowatch").map { status ->
                pool.submit(Callable { start.await(); client.library("shows", status) })
            }
            start.countDown()
            val rows = futures.map { it.get(5, TimeUnit.SECONDS) }
            assertEquals(listOf("Watching", "Planned"), rows.map { it.items.single().title })
            assertEquals(1, counts.getValue("/sync/activities").get())
            assertEquals(1, counts.getValue("/sync/all-items/shows").get())
            assertTrue(java.io.File(dir, "library-shows.json").exists())
        } finally { pool.shutdownNow(); dir.deleteRecursively() }
    }
}
