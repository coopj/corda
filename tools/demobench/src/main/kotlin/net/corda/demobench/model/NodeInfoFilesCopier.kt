package net.corda.demobench.model

import net.corda.core.internal.isDirectory
import net.corda.core.internal.isRegularFile
import net.corda.core.utilities.loggerFor
import rx.Observable
import rx.Subscription
import java.nio.file.*
import java.util.concurrent.TimeUnit


private data class WatchTarget(val path : Path, val watchService: WatchService)

/**
 * Utility class which copies nodeInfo files across a set of running nodes.
 * 
 */
class NodeInfoFilesCopier {

    companion object {
        private val logger = loggerFor<NodeInfoFilesCopier>()
    }

    private val destinations = mutableListOf<Path>()
    private val watchTargets = mutableListOf<WatchTarget>()
    private val subscription : Subscription

    private val previouslySeenFiles = mutableListOf<Path>()

    init {
        this.subscription = Observable.interval(5, TimeUnit.SECONDS)
                .subscribe { poll() }
    }

    fun addConfig(nodeConfig: NodeConfig) {
        addDestination(nodeConfig.nodeDir.resolve("additional-node-infos"))
        watchTargets.add(WatchTarget(nodeConfig.nodeDir, initWatch(nodeConfig.nodeDir)!!))
    }

    private fun initWatch(path : Path) : WatchService? {
       if (!path.isDirectory()) {
           logger.info("Creating $path")
           path.toFile().mkdirs()
        }
        val watchService = path.fileSystem.newWatchService()
        path.register(watchService, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_MODIFY)
        logger.info("now watching $path")
        return watchService
    }

    private fun poll() {
        for (watchTarget in watchTargets) {
            val watchKey: WatchKey? = watchTarget.watchService.poll()
            // This can happen and it means that there are no events.
            if (watchKey == null) return

            for (event in watchKey.pollEvents()) {
                val kind = event.kind()
                if (kind == StandardWatchEventKinds.OVERFLOW) continue

                @Suppress("UNCHECKED_CAST")
                val fileName : Path = (event as WatchEvent<Path>).context()
                val fullPath = watchTarget.path.resolve(fileName)
                if (fullPath.isRegularFile() && fileName.toString().startsWith("nodeInfo-")) {
                    previouslySeenFiles.add(fullPath)
                    for (destination in destinations) {
                        val fullDestination = destination.resolve(fileName)
                        copy(fullPath, fullDestination)
                    }
                }
            }
            if (!watchKey.reset()) {
                break
            }
        }
    }

    private fun copy(source : Path, destination: Path) {
        try {
            Files.copy(source, destination, StandardCopyOption.REPLACE_EXISTING)
        } catch (e : Exception) {
            logger.warn("Couldn't copy $source to $destination. Exception: ${e.toString()}")
        }
    }

    private fun addDestination(destination : Path) {
        destination.toFile().mkdirs()
        destinations.add(destination)

        for (previouslySeenFile in previouslySeenFiles) {
            copy(previouslySeenFile, destination.resolve(previouslySeenFile.fileName))
        }
    }

}