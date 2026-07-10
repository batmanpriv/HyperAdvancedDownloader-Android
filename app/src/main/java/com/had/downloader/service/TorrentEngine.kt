package com.had.downloader.service

import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.io.*
import java.net.*
import java.nio.ByteBuffer
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.min
import kotlin.random.Random

private const val TAG = "TorrentEngine"
private const val BLOCK_SIZE = 16384
private const val MAX_PEERS = 50
private const val HANDSHAKE_TIMEOUT = 5000
private const val MAX_PIPELINE = 10
private const val UNCHOKE_INTERVAL_MS = 10_000L
private const val ENDGAME_THRESHOLD = 0.95f

data class TorrentInfo(
    val infoHash: ByteArray,
    val infoHashHex: String,
    val name: String,
    val totalSize: Long,
    val pieceLength: Long,
    val pieces: List<ByteArray>,
    val files: List<TorrentFile>,
    val trackers: List<String>,
    val isMultiFile: Boolean,
    val comment: String = "",
    val createdBy: String = "",
    val creationDate: Long = 0L
) {
    override fun equals(other: Any?) = other is TorrentInfo && infoHashHex == other.infoHashHex
    override fun hashCode() = infoHashHex.hashCode()
}

data class TorrentFile(
    val path: String,
    val length: Long,
    val startPiece: Int,
    val endPiece: Int,
    var selected: Boolean = true,
    val startOffset: Long = 0L
)

data class PeerInfo(
    val ip: String,
    val port: Int,
    var connected: Boolean = false,
    var uploadSpeed: Long = 0,
    var downloadSpeed: Long = 0,
    var choking: Boolean = true,
    var interested: Boolean = false,
    var client: String = "Unknown",
    var piecesHave: Int = 0,
    var totalPieces: Int = 0,
    var lastSeen: Long = System.currentTimeMillis(),
    var bitfield: ByteArray = ByteArray(0),
    var pendingRequests: Int = 0,
    var successfulBlocks: Int = 0
)

data class TorrentProgress(
    val infoHashHex: String,
    val name: String,
    val totalBytes: Long,
    val downloadedBytes: Long,
    val uploadedBytes: Long,
    val speedBps: Long,
    val uploadSpeedBps: Long,
    val peers: List<PeerInfo>,
    val connectedPeers: Int,
    val seeders: Int,
    val leechers: Int,
    val pieces: Int,
    val completedPieces: Int,
    val eta: Int,
    val percent: Float,
    val status: String,
    val files: List<TorrentFile> = emptyList(),
    val errorMessage: String? = null
)

data class MagnetInfo(
    val infoHash: String,
    val name: String,
    val trackers: List<String>,
    val size: Long = 0
)

data class PieceState(
    val index: Int,
    var done: Boolean = false,
    var downloading: Boolean = false,
    var failCount: Int = 0,
    var rarity: Int = 0
)

@Singleton
class TorrentEngine @Inject constructor() {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val peerId = generatePeerId()
    private val activeTorrents = ConcurrentHashMap<String, TorrentSession>()

    private val _progress = MutableSharedFlow<TorrentProgress>(extraBufferCapacity = 256)
    val progress: SharedFlow<TorrentProgress> = _progress.asSharedFlow()

    fun parseMagnet(magnet: String): MagnetInfo? = runCatching {
        val uri = URI(magnet)
        val params = parseQueryParams(uri.schemeSpecificPart.removePrefix("?"))
        val xt = params["xt"] ?: return null
        val infoHash = xt.removePrefix("urn:btih:").lowercase()
        val name = params["dn"]?.let { URLDecoder.decode(it, "UTF-8") } ?: infoHash.take(8)
        val trackers = params.entries
            .filter { it.key == "tr" }
            .map { URLDecoder.decode(it.value, "UTF-8") }
        MagnetInfo(infoHash, name, trackers)
    }.getOrNull()

    fun parseTorrentFile(bytes: ByteArray): TorrentInfo? = runCatching {
        val decoder = BencodeDecoder(bytes)
        val root = decoder.decode() as? Map<*, *> ?: return null
        val info = root["info"] as? Map<*, *> ?: return null

        val infoBytes = extractInfoBytes(bytes)
        val infoHash = sha1(infoBytes)
        val infoHashHex = infoHash.hex()

        val name = (info["name"] as? ByteArray)?.toString(Charsets.UTF_8) ?: "Unknown"
        val pieceLength = (info["piece length"] as? Long) ?: 262144L
        val piecesRaw = (info["pieces"] as? ByteArray) ?: return null
        val pieces = (0 until piecesRaw.size / 20).map { i ->
            piecesRaw.copyOfRange(i * 20, i * 20 + 20)
        }

        val comment = (root["comment"] as? ByteArray)?.toString(Charsets.UTF_8) ?: ""
        val createdBy = (root["created by"] as? ByteArray)?.toString(Charsets.UTF_8) ?: ""
        val creationDate = (root["creation date"] as? Long) ?: 0L

        val trackers = mutableListOf<String>()
        (root["announce"] as? ByteArray)?.toString(Charsets.UTF_8)?.let { trackers.add(it) }
        (root["announce-list"] as? List<*>)?.forEach { tier ->
            (tier as? List<*>)?.forEach { url ->
                (url as? ByteArray)?.toString(Charsets.UTF_8)?.let { trackers.add(it) }
            }
        }

        val files = mutableListOf<TorrentFile>()
        val fileList = info["files"] as? List<*>
        if (fileList != null) {
            var offset = 0L
            fileList.forEach { fileMap ->
                val fm = fileMap as? Map<*, *> ?: return@forEach
                val length = (fm["length"] as? Long) ?: 0L
                val pathParts = (fm["path"] as? List<*>)?.mapNotNull {
                    (it as? ByteArray)?.toString(Charsets.UTF_8)
                } ?: listOf("file")
                val path = pathParts.joinToString("/")
                val startPiece = (offset / pieceLength).toInt()
                val endPiece = ((offset + length - 1) / pieceLength).toInt()
                files.add(TorrentFile(path, length, startPiece, endPiece, true, offset))
                offset += length
            }
        } else {
            val length = (info["length"] as? Long) ?: 0L
            files.add(TorrentFile(name, length, 0, (length / pieceLength).toInt(), true, 0L))
        }

        val totalSize = files.sumOf { it.length }
        TorrentInfo(infoHash, infoHashHex, name, totalSize, pieceLength, pieces, files, trackers, fileList != null, comment, createdBy, creationDate)
    }.getOrNull()

    fun startDownload(torrentInfo: TorrentInfo, outputDir: String) {
        activeTorrents[torrentInfo.infoHashHex]?.stop()
        val session = TorrentSession(torrentInfo, outputDir, peerId, scope)
        activeTorrents[torrentInfo.infoHashHex] = session
        scope.launch {
            session.start { prog -> _progress.emit(prog) }
        }
    }

    fun startMagnetDownload(magnetInfo: MagnetInfo, outputDir: String) {
        scope.launch {
            val infoHash = magnetInfo.infoHash.hexToBytes()
            emitStatus(magnetInfo.infoHash, magnetInfo.name, "FETCHING_METADATA")
            val torrentInfo = fetchMetadataViaDHT(infoHash, magnetInfo)
            if (torrentInfo == null) {
                emitStatus(magnetInfo.infoHash, magnetInfo.name, "FAILED: Could not fetch metadata")
                return@launch
            }
            startDownload(torrentInfo, outputDir)
        }
    }

    fun stopDownload(infoHashHex: String) {
        activeTorrents[infoHashHex]?.stop()
        activeTorrents.remove(infoHashHex)
    }

    fun stopAll() {
        activeTorrents.values.forEach { it.stop() }
        activeTorrents.clear()
    }

    fun getPeers(infoHashHex: String): List<PeerInfo> = activeTorrents[infoHashHex]?.getPeers() ?: emptyList()
    fun getTorrentInfo(infoHashHex: String): TorrentInfo? = activeTorrents[infoHashHex]?.info
    fun getActiveTorrents(): List<String> = activeTorrents.keys.toList()

    fun updateFileSelection(infoHashHex: String, fileIndex: Int, selected: Boolean) {
        activeTorrents[infoHashHex]?.updateFileSelection(fileIndex, selected)
    }

    private suspend fun fetchMetadataViaDHT(infoHash: ByteArray, magnetInfo: MagnetInfo): TorrentInfo? =
        withContext(Dispatchers.IO) {
            val peers = mutableListOf<PeerInfo>()
            magnetInfo.trackers.forEach { tracker ->
                runCatching {
                    peers.addAll(announceToTracker(tracker, infoHash, 0L, 0L, 0L, 0))
                }
            }
            if (peers.isEmpty()) peers.addAll(dhtGetPeers(infoHash))
            for (peer in peers.take(15)) {
                val info = runCatching { fetchExtensionMetadata(peer, infoHash) }.getOrNull()
                if (info != null) return@withContext info
            }
            null
        }

    private fun fetchExtensionMetadata(peer: PeerInfo, infoHash: ByteArray): TorrentInfo? {
        val socket = Socket()
        socket.connect(InetSocketAddress(peer.ip, peer.port), HANDSHAKE_TIMEOUT)
        socket.soTimeout = 10000
        val out = socket.getOutputStream()
        val inp = socket.getInputStream()

        val handshake = buildHandshake(infoHash, peerId, supportsExtension = true)
        out.write(handshake)
        val response = ByteArray(68)
        var read = 0
        while (read < 68) {
            val n = inp.read(response, read, 68 - read)
            if (n < 0) { socket.close(); return null }
            read += n
        }
        if (!response.copyOfRange(28, 48).contentEquals(infoHash)) { socket.close(); return null }

        val extHandshake = buildExtensionHandshake()
        out.write(extHandshake)

        val metadataPieces = mutableMapOf<Int, ByteArray>()
        var totalPieces = -1
        var metadataSize = 0
        var utMetadataId = 1

        repeat(100) {
            val msg = readMessage(inp) ?: return null
            if (msg.isEmpty()) return@repeat
            val msgId = msg[0].toInt() and 0xFF
            if (msgId == 20) {
                val extId = msg[1].toInt() and 0xFF
                val payload = msg.copyOfRange(2, msg.size)
                val decoderInner = BencodeDecoder(payload)
                val decoded = decoderInner.decode() as? Map<*, *> ?: return@repeat
                val dictEnd = decoderInner.decodeWithOffset()
                if (extId == 0) {
                    val mDict = decoded["m"] as? Map<*, *>
                    utMetadataId = ((mDict?.get("ut_metadata") as? Long) ?: 1L).toInt()
                    metadataSize = ((decoded["metadata_size"] as? Long) ?: 0L).toInt()
                    totalPieces = (metadataSize + 16383) / 16384
                    if (metadataSize > 0) {
                        for (i in 0 until totalPieces) {
                            val req = buildMetadataRequest(utMetadataId, i)
                            out.write(req)
                        }
                    }
                } else if (extId == utMetadataId) {
                    val msgType = (decoded["msg_type"] as? Long)?.toInt()
                    val piece = (decoded["piece"] as? Long)?.toInt() ?: 0
                    if (msgType == 1 && payload.size > dictEnd) {
                        metadataPieces[piece] = payload.copyOfRange(dictEnd, payload.size)
                    }
                }
            }
            if (totalPieces > 0 && metadataPieces.size >= totalPieces) {
                socket.close()
                val metaBytes = (0 until totalPieces).map { i ->
                    metadataPieces[i] ?: return null
                }.fold(ByteArray(0)) { acc, b -> acc + b }
                return parseTorrentFile(buildTorrentBytes(metaBytes))
            }
        }
        socket.close()
        return null
    }

    suspend fun announceToTracker(
        tracker: String, infoHash: ByteArray,
        downloaded: Long, uploaded: Long, left: Long, port: Int
    ): List<PeerInfo> = withContext(Dispatchers.IO) {
        when {
            tracker.startsWith("udp://") -> announceUDP(tracker, infoHash, downloaded, uploaded, left, port)
            tracker.startsWith("http") -> announceHTTP(tracker, infoHash, downloaded, uploaded, left, port)
            else -> emptyList()
        }
    }

    private fun announceHTTP(
        tracker: String, infoHash: ByteArray,
        downloaded: Long, uploaded: Long, left: Long, port: Int
    ): List<PeerInfo> = runCatching {
        val hash = URLEncoder.encode(String(infoHash.map { it.toChar() }.toCharArray()), "ISO-8859-1")
        val pid = URLEncoder.encode(String(peerId.map { it.toChar() }.toCharArray()), "ISO-8859-1")
        val url = "$tracker?info_hash=$hash&peer_id=$pid&port=${if (port == 0) 6881 else port}" +
                "&uploaded=$uploaded&downloaded=$downloaded&left=$left&compact=1&numwant=80"
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.connectTimeout = 8000
        conn.readTimeout = 8000
        conn.setRequestProperty("User-Agent", "HAD/2.0")
        conn.connect()
        if (conn.responseCode != 200) { conn.disconnect(); return emptyList() }
        val data = conn.inputStream.readBytes()
        conn.disconnect()
        val response = BencodeDecoder(data).decode() as? Map<*, *> ?: return emptyList()
        val peersData = response["peers"]
        when (peersData) {
            is ByteArray -> parseBinaryPeers(peersData)
            is List<*> -> parseDictPeers(peersData)
            else -> emptyList()
        }
    }.getOrDefault(emptyList())

    private fun parseDictPeers(list: List<*>): List<PeerInfo> {
        return list.mapNotNull { item ->
            val m = item as? Map<*, *> ?: return@mapNotNull null
            val ip = (m["ip"] as? ByteArray)?.toString(Charsets.UTF_8) ?: return@mapNotNull null
            val port = (m["port"] as? Long)?.toInt() ?: return@mapNotNull null
            PeerInfo(ip, port)
        }
    }

    private fun announceUDP(
        tracker: String, infoHash: ByteArray,
        downloaded: Long, uploaded: Long, left: Long, port: Int
    ): List<PeerInfo> = runCatching {
        val uri = URI(tracker)
        val socket = DatagramSocket()
        socket.soTimeout = 5000
        val addr = InetAddress.getByName(uri.host)
        val trackerPort = if (uri.port > 0) uri.port else 80

        val connectId = 0x41727101980L
        val transId = Random.nextInt()
        val connectReq = ByteBuffer.allocate(16).apply {
            putLong(connectId); putInt(0); putInt(transId)
        }.array()
        socket.send(DatagramPacket(connectReq, 16, addr, trackerPort))

        val respBuf = ByteArray(16)
        socket.receive(DatagramPacket(respBuf, 16))
        val buf0 = ByteBuffer.wrap(respBuf)
        buf0.getInt(); buf0.getInt()
        val connId = buf0.getLong()

        val transId2 = Random.nextInt()
        val announceReq = ByteBuffer.allocate(98).apply {
            putLong(connId); putInt(1); putInt(transId2)
            put(infoHash); put(peerId)
            putLong(downloaded); putLong(left); putLong(uploaded)
            putInt(0); putLong(0); putInt(0); putInt(80)
            putShort((if (port == 0) 6881 else port).toShort())
        }.array()
        socket.send(DatagramPacket(announceReq, 98, addr, trackerPort))

        val respBuf2 = ByteArray(1320)
        val resp = DatagramPacket(respBuf2, 1320)
        socket.receive(resp)
        socket.close()

        val buf = ByteBuffer.wrap(respBuf2, 0, resp.length)
        repeat(5) { buf.getInt() }
        val peers = mutableListOf<PeerInfo>()
        while (buf.remaining() >= 6) {
            val ip = "${buf.get().toInt() and 0xFF}.${buf.get().toInt() and 0xFF}" +
                    ".${buf.get().toInt() and 0xFF}.${buf.get().toInt() and 0xFF}"
            val p = (buf.short.toInt() and 0xFFFF)
            if (p > 0) peers.add(PeerInfo(ip, p))
        }
        peers
    }.getOrDefault(emptyList())

    private fun dhtGetPeers(infoHash: ByteArray): List<PeerInfo> {
        val bootstrapNodes = listOf(
            "router.bittorrent.com" to 6881,
            "router.utorrent.com" to 6881,
            "dht.transmissionbt.com" to 6881,
            "dht.aelitis.com" to 6881
        )
        val peers = mutableListOf<PeerInfo>()
        val nodeId = Random.nextBytes(20)

        bootstrapNodes.forEach { (host, port) ->
            runCatching {
                val socket = DatagramSocket()
                socket.soTimeout = 3000
                val addr = InetAddress.getByName(host)

                val tid = "aa"
                val hashStr = String(infoHash.map { it.toChar() }.toCharArray())
                val nodeStr = String(nodeId.map { it.toChar() }.toCharArray())
                val msg = "d1:ad2:id20:${nodeStr}9:info_hash20:${hashStr}e1:q9:get_peers1:t2:${tid}1:y1:qe"
                val msgBytes = msg.toByteArray(Charsets.ISO_8859_1)
                socket.send(DatagramPacket(msgBytes, msgBytes.size, addr, port))

                val resp = ByteArray(1500)
                val pkt = DatagramPacket(resp, 1500)
                socket.receive(pkt)
                socket.close()

                val decoded = BencodeDecoder(resp.copyOfRange(0, pkt.length)).decode() as? Map<*, *>
                val r = decoded?.get("r") as? Map<*, *>

                val values = r?.get("values") as? List<*>
                values?.forEach { v ->
                    (v as? ByteArray)?.let { peers.addAll(parseBinaryPeers(it)) }
                }

                val nodes = r?.get("nodes") as? ByteArray
                nodes?.let {
                    val buf = ByteBuffer.wrap(it)
                    while (buf.remaining() >= 26) {
                        val nodeIdBytes = ByteArray(20)
                        buf.get(nodeIdBytes)
                        val ip = "${buf.get().toInt() and 0xFF}.${buf.get().toInt() and 0xFF}" +
                                ".${buf.get().toInt() and 0xFF}.${buf.get().toInt() and 0xFF}"
                        val nodePort = buf.short.toInt() and 0xFFFF
                        if (nodePort > 0 && peers.size < 50) {
                            runCatching {
                                val s2 = DatagramSocket()
                                s2.soTimeout = 2000
                                val a2 = InetAddress.getByName(ip)
                                val msg2 = "d1:ad2:id20:${nodeStr}9:info_hash20:${hashStr}e1:q9:get_peers1:t2:${tid}1:y1:qe"
                                val mb2 = msg2.toByteArray(Charsets.ISO_8859_1)
                                s2.send(DatagramPacket(mb2, mb2.size, a2, nodePort))
                                val r2 = ByteArray(1500)
                                val p2 = DatagramPacket(r2, 1500)
                                s2.receive(p2)
                                s2.close()
                                val d2 = BencodeDecoder(r2.copyOfRange(0, p2.length)).decode() as? Map<*, *>
                                val r2map = d2?.get("r") as? Map<*, *>
                                val vals2 = r2map?.get("values") as? List<*>
                                vals2?.forEach { v2 ->
                                    (v2 as? ByteArray)?.let { peers.addAll(parseBinaryPeers(it)) }
                                }
                            }
                        }
                    }
                }
            }
        }
        return peers.distinctBy { "${it.ip}:${it.port}" }
    }

    private fun parseBinaryPeers(data: ByteArray): List<PeerInfo> {
        val peers = mutableListOf<PeerInfo>()
        val buf = ByteBuffer.wrap(data)
        while (buf.remaining() >= 6) {
            val ip = "${buf.get().toInt() and 0xFF}.${buf.get().toInt() and 0xFF}" +
                    ".${buf.get().toInt() and 0xFF}.${buf.get().toInt() and 0xFF}"
            val port = buf.short.toInt() and 0xFFFF
            if (port > 0) peers.add(PeerInfo(ip, port))
        }
        return peers
    }

    fun buildHandshake(infoHash: ByteArray, peerId: ByteArray, supportsExtension: Boolean = false): ByteArray {
        val buf = ByteBuffer.allocate(68)
        buf.put(19)
        buf.put("BitTorrent protocol".toByteArray())
        val reserved = ByteArray(8)
        if (supportsExtension) reserved[5] = 0x10
        buf.put(reserved)
        buf.put(infoHash)
        buf.put(peerId)
        return buf.array()
    }

    private fun buildExtensionHandshake(): ByteArray {
        val dict = "d1:md11:ut_metadatai1eee"
        val payload = byteArrayOf(20, 0) + dict.toByteArray()
        return ByteBuffer.allocate(4 + payload.size).putInt(payload.size).put(payload).array()
    }

    private fun buildMetadataRequest(extId: Int, piece: Int): ByteArray {
        val dict = "d8:msg_typei0e5:piecei${piece}ee"
        val payload = byteArrayOf(20, extId.toByte()) + dict.toByteArray()
        return ByteBuffer.allocate(4 + payload.size).putInt(payload.size).put(payload).array()
    }

    private fun buildTorrentBytes(infoBytes: ByteArray): ByteArray =
        "d4:info".toByteArray() + infoBytes + "e".toByteArray()

    private fun readMessage(inp: InputStream): ByteArray? = runCatching {
        val lenBuf = ByteArray(4)
        var read = 0
        while (read < 4) {
            val n = inp.read(lenBuf, read, 4 - read)
            if (n < 0) return null
            read += n
        }
        val len = ByteBuffer.wrap(lenBuf).int
        if (len == 0) return ByteArray(0)
        if (len > 1 shl 22) return null
        val data = ByteArray(len)
        var totalRead = 0
        while (totalRead < len) {
            val n = inp.read(data, totalRead, len - totalRead)
            if (n < 0) return null
            totalRead += n
        }
        data
    }.getOrNull()

    private fun extractInfoBytes(torrent: ByteArray): ByteArray {
        val marker = "4:info".toByteArray()
        val idx = torrent.indexOf(marker)
        if (idx < 0) return ByteArray(0)
        val start = idx + marker.size
        val sub = torrent.copyOfRange(start, torrent.size)
        val dec = BencodeDecoder(sub)
        val end = runCatching { dec.decodeWithOffset() }.getOrDefault(sub.size)
        return sub.copyOfRange(0, end)
    }

    private fun sha1(data: ByteArray): ByteArray = MessageDigest.getInstance("SHA-1").digest(data)
    private fun ByteArray.hex() = joinToString("") { "%02x".format(it) }
    private fun String.hexToBytes(): ByteArray {
        return if (length == 40) {
            chunked(2).map { it.toInt(16).toByte() }.toByteArray()
        } else {
            toByteArray(Charsets.ISO_8859_1)
        }
    }

    private fun ByteArray.indexOf(sub: ByteArray): Int {
        outer@ for (i in 0..size - sub.size) {
            for (j in sub.indices) if (this[i + j] != sub[j]) continue@outer
            return i
        }
        return -1
    }

    private fun parseQueryParams(query: String): Map<String, String> {
        val map = mutableMapOf<String, String>()
        query.split("&").forEach { part ->
            val eq = part.indexOf('=')
            if (eq > 0) map[part.substring(0, eq)] = part.substring(eq + 1)
        }
        return map
    }

    private fun generatePeerId(): ByteArray {
        val prefix = "-HD0200-".toByteArray()
        val random = Random.nextBytes(12)
        return prefix + random
    }

    private suspend fun emitStatus(hash: String, name: String, status: String) {
        _progress.emit(
            TorrentProgress(hash, name, 0, 0, 0, 0, 0, emptyList(), 0, 0, 0, 0, 0, -1, 0f, status)
        )
    }
}

class TorrentSession(
    val info: TorrentInfo,
    val outputDir: String,
    val peerId: ByteArray,
    val scope: CoroutineScope
) {
    private var job: Job? = null
    private val peers = ConcurrentHashMap<String, PeerInfo>()
    private val pieceStates = Array(info.pieces.size) { i -> PieceState(i) }
    private val peerPieceCount = ConcurrentHashMap<Int, Int>()
    private val downloadedBytes = AtomicLong(0)
    private val uploadedBytes = AtomicLong(0)
    private val speedTracker = SpeedTracker()
    private val activePeerCount = AtomicInteger(0)
    private val unchokedPeers = mutableSetOf<String>()
    private val unchokedLock = Any()

    @Volatile private var stopped = false
    @Volatile private var fileSelection = info.files.map { it.selected }.toBooleanArray()

    fun updateFileSelection(fileIndex: Int, selected: Boolean) {
        if (fileIndex in fileSelection.indices) fileSelection[fileIndex] = selected
    }

    fun start(onProgress: suspend (TorrentProgress) -> Unit) {
        job = scope.launch {
            try {
                val engine = TorrentEngine()
                emitProgress(onProgress, "CONNECTING")

                val allPeers = mutableListOf<PeerInfo>()
                info.trackers.map { tracker ->
                    async {
                        runCatching {
                            engine.announceToTracker(tracker, info.infoHash, 0, 0, info.totalSize, 6881)
                        }.getOrDefault(emptyList())
                    }
                }.awaitAll().forEach { allPeers.addAll(it) }

                allPeers.distinctBy { "${it.ip}:${it.port}" }.forEach {
                    peers["${it.ip}:${it.port}"] = it
                }

                if (peers.isEmpty()) {
                    emitProgress(onProgress, "FAILED: No peers found")
                    return@launch
                }

                emitProgress(onProgress, "DOWNLOADING")

                val files = prepareFiles()

                initBitfieldFromFiles(files)

                val neededPieces = determineNeededPieces()

                scope.launch { unchokeLoop() }

                val semaphore = Semaphore(8)

                val isEndgame = { completedPct: Float -> completedPct >= ENDGAME_THRESHOLD }
                val completedCount = AtomicInteger(pieceStates.count { it.done })

                while (!stopped) {
                    val remaining = neededPieces.filter { !pieceStates[it].done && !pieceStates[it].downloading }
                    if (remaining.isEmpty()) break

                    val currentPct = completedCount.get().toFloat() / neededPieces.size
                    val endgame = isEndgame(currentPct)

                    val sortedPieces = if (endgame) {
                        remaining
                    } else {
                        remaining.sortedBy { peerPieceCount[it] ?: Int.MAX_VALUE }
                    }

                    val batch = sortedPieces.take(if (endgame) remaining.size else 16)

                    batch.map { pieceIdx ->
                        async {
                            semaphore.withPermit {
                                if (stopped || pieceStates[pieceIdx].done) return@withPermit
                                val peer = selectBestPeerForPiece(pieceIdx) ?: return@withPermit
                                pieceStates[pieceIdx].downloading = true
                                runCatching {
                                    downloadPiece(pieceIdx, peer, files)
                                    if (pieceStates[pieceIdx].done) {
                                        completedCount.incrementAndGet()
                                        emitProgress(onProgress, "DOWNLOADING")
                                    }
                                }.onFailure { e ->
                                    pieceStates[pieceIdx].downloading = false
                                    pieceStates[pieceIdx].failCount++
                                    Log.w(TAG, "Piece $pieceIdx failed: ${e.message}")
                                    if (pieceStates[pieceIdx].failCount > 3) {
                                        peer.successfulBlocks = 0
                                    }
                                }
                            }
                        }
                    }.awaitAll()

                    if (completedCount.get() >= neededPieces.size) break
                    delay(50L)
                }

                files.forEach { (_, raf) -> runCatching { raf.close() } }

                if (!stopped) emitProgress(onProgress, "COMPLETED")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Session error: ${e.message}")
                onProgress(buildProgress("FAILED: ${e.message}"))
            }
        }
    }

    private fun initBitfieldFromFiles(files: List<Pair<TorrentFile, RandomAccessFile>>) {
        files.forEach { (torrentFile, raf) ->
            if (torrentFile.length == 0L) return@forEach
            runCatching {
                val fileLen = raf.length()
                if (fileLen == torrentFile.length) {
                    for (p in torrentFile.startPiece..torrentFile.endPiece) {
                        if (p < pieceStates.size) pieceStates[p].done = true
                    }
                }
            }
        }
    }

    private suspend fun unchokeLoop() {
        while (!stopped) {
            delay(UNCHOKE_INTERVAL_MS)
            val connectedPeers = peers.values.filter { it.connected }
            connectedPeers.sortedByDescending { it.downloadSpeed }.take(4).forEach { peer ->
                val key = "${peer.ip}:${peer.port}"
                synchronized(unchokedLock) { unchokedPeers.add(key) }
                peer.choking = false
            }
            val topKeys = connectedPeers.sortedByDescending { it.downloadSpeed }.take(4)
                .map { "${it.ip}:${it.port}" }.toSet()
            synchronized(unchokedLock) {
                val toChoke = unchokedPeers - topKeys
                toChoke.forEach { key ->
                    peers[key]?.choking = true
                    unchokedPeers.remove(key)
                }
            }
        }
    }

    private fun selectBestPeerForPiece(pieceIdx: Int): PeerInfo? {
        val now = System.currentTimeMillis()
        return peers.values
            .filter { peer ->
                val hasPiece = peer.bitfield.isNotEmpty() &&
                        (pieceIdx / 8 < peer.bitfield.size) &&
                        ((peer.bitfield[pieceIdx / 8].toInt() and (0x80 shr (pieceIdx % 8))) != 0)
                val isUsable = (!peer.choking || now - peer.lastSeen > 30_000) &&
                        peer.pendingRequests < MAX_PIPELINE
                hasPiece && isUsable
            }
            .maxByOrNull { it.successfulBlocks * 2 + it.downloadSpeed }
            ?: peers.values.filter { it.pendingRequests < MAX_PIPELINE }.maxByOrNull { it.downloadSpeed }
    }

    private fun determineNeededPieces(): List<Int> {
        val needed = mutableSetOf<Int>()
        info.files.forEachIndexed { idx, file ->
            if (fileSelection.getOrElse(idx) { true }) {
                for (p in file.startPiece..file.endPiece) {
                    if (p < pieceStates.size) needed.add(p)
                }
            }
        }
        return needed.toList().sorted()
    }

    private fun prepareFiles(): List<Pair<TorrentFile, RandomAccessFile>> {
        val dir = if (info.isMultiFile) File(outputDir, sanitizeName(info.name)) else File(outputDir)
        dir.mkdirs()
        return info.files.mapIndexed { idx, f ->
            val shouldCreate = fileSelection.getOrElse(idx) { true }
            val file = if (info.isMultiFile) {
                File(dir, f.path.split("/").joinToString(File.separator) { sanitizeName(it) })
                    .also { it.parentFile?.mkdirs() }
            } else {
                File(outputDir, sanitizeName(f.path))
            }
            val raf = RandomAccessFile(file, "rw")
            if (shouldCreate && file.length() != f.length) raf.setLength(f.length)
            f to raf
        }
    }

    private fun sanitizeName(name: String): String =
        name.replace(Regex("[\\\\/:*?\"<>|]"), "_").trim()

    private suspend fun downloadPiece(
        pieceIdx: Int, peer: PeerInfo,
        files: List<Pair<TorrentFile, RandomAccessFile>>
    ) = withContext(Dispatchers.IO) {
        if (pieceStates[pieceIdx].done) return@withContext
        pieceStates[pieceIdx].downloading = true

        val pieceStart = pieceIdx.toLong() * info.pieceLength
        val pieceEnd = min(pieceStart + info.pieceLength, info.totalSize)
        val pieceLen = (pieceEnd - pieceStart).toInt()

        val socket = Socket()
        socket.connect(InetSocketAddress(peer.ip, peer.port), 5000)
        socket.soTimeout = 30000
        val out = socket.getOutputStream()
        val inp = socket.getInputStream()

        try {
            val engine = TorrentEngine()
            val handshake = engine.buildHandshake(info.infoHash, peerId)
            out.write(handshake)

            val resp = ByteArray(68)
            var read = 0
            while (read < 68) {
                val n = inp.read(resp, read, 68 - read)
                if (n < 0) throw IOException("Handshake failed")
                read += n
            }

            peer.connected = true
            peer.lastSeen = System.currentTimeMillis()

            if (peer.bitfield.isEmpty() && info.pieces.isNotEmpty()) {
                val bitfieldLen = (info.pieces.size + 7) / 8
                sendBitfield(out, ByteArray(bitfieldLen))
            }
            sendInterested(out)

            var unchokeReceived = false
            val startWait = System.currentTimeMillis()
            while (!unchokeReceived && System.currentTimeMillis() - startWait < 15000) {
                val msg = readMsg(inp) ?: break
                if (msg.isEmpty()) continue
                when (msg[0].toInt() and 0xFF) {
                    1 -> unchokeReceived = true
                    4 -> {
                        val haveIdx = if (msg.size >= 5) ByteBuffer.wrap(msg, 1, 4).int else -1
                        if (haveIdx >= 0 && haveIdx < info.pieces.size) {
                            peer.piecesHave++
                            val pieceByte = haveIdx / 8
                            val pieceBit = 0x80 shr (haveIdx % 8)
                            if (pieceByte < peer.bitfield.size) {
                                peer.bitfield[pieceByte] = (peer.bitfield[pieceByte].toInt() or pieceBit).toByte()
                            }
                            peerPieceCount[haveIdx] = (peerPieceCount[haveIdx] ?: 0) + 1
                            peer.lastSeen = System.currentTimeMillis()
                        }
                    }
                    5 -> {
                        if (msg.size > 1) {
                            peer.bitfield = msg.copyOfRange(1, msg.size)
                            peer.piecesHave = peer.bitfield.sumOf { b ->
                                Integer.bitCount(b.toInt() and 0xFF)
                            }
                            peer.totalPieces = info.pieces.size
                            peer.bitfield.forEachIndexed { byteIdx, byte ->
                                for (bit in 0..7) {
                                    val pieceIdx2 = byteIdx * 8 + bit
                                    if (pieceIdx2 < info.pieces.size && (byte.toInt() and (0x80 shr bit)) != 0) {
                                        peerPieceCount[pieceIdx2] = (peerPieceCount[pieceIdx2] ?: 0) + 1
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (!unchokeReceived) {
                pieceStates[pieceIdx].downloading = false
                return@withContext
            }

            val pieceData = ByteArray(pieceLen)
            var offset = 0
            peer.pendingRequests = 0

            while (offset < pieceLen) {
                val pending = min(MAX_PIPELINE, (pieceLen - offset + BLOCK_SIZE - 1) / BLOCK_SIZE)
                repeat(pending) { i ->
                    val blockOffset = offset + i * BLOCK_SIZE
                    if (blockOffset < pieceLen) {
                        val blockSize = min(BLOCK_SIZE, pieceLen - blockOffset)
                        requestBlock(out, pieceIdx, blockOffset, blockSize)
                        peer.pendingRequests++
                    }
                }

                repeat(pending) {
                    val block = readPieceBlock(inp) ?: return@withContext
                    val (bPiece, bOffset, bData) = block
                    if (bPiece == pieceIdx && bOffset + bData.size <= pieceLen) {
                        System.arraycopy(bData, 0, pieceData, bOffset, bData.size)
                        downloadedBytes.addAndGet(bData.size.toLong())
                        speedTracker.add(bData.size.toLong())
                        peer.successfulBlocks++
                        peer.pendingRequests = maxOf(0, peer.pendingRequests - 1)
                    }
                }
                offset += pending * BLOCK_SIZE
            }

            if (verifySha1(pieceData, info.pieces[pieceIdx])) {
                writeToFiles(pieceIdx, pieceData, files)
                pieceStates[pieceIdx].done = true
                pieceStates[pieceIdx].downloading = false
                peer.downloadSpeed = speedTracker.getSpeed()
                peer.lastSeen = System.currentTimeMillis()
            } else {
                pieceStates[pieceIdx].failCount++
                pieceStates[pieceIdx].downloading = false
                Log.w(TAG, "SHA1 mismatch piece $pieceIdx from ${peer.ip}")
            }
        } finally {
            peer.pendingRequests = 0
            pieceStates[pieceIdx].downloading = false
            runCatching { socket.close() }
        }
    }

    private fun sendBitfield(out: OutputStream, bitfield: ByteArray) {
        val payload = byteArrayOf(5) + bitfield
        out.write(ByteBuffer.allocate(4 + payload.size).putInt(payload.size).put(payload).array())
    }

    private fun sendInterested(out: OutputStream) {
        out.write(ByteBuffer.allocate(5).putInt(1).put(2).array())
    }

    private fun requestBlock(out: OutputStream, piece: Int, offset: Int, length: Int) {
        out.write(
            ByteBuffer.allocate(17).putInt(13).put(6).putInt(piece).putInt(offset).putInt(length).array()
        )
    }

    private fun readPieceBlock(inp: InputStream): Triple<Int, Int, ByteArray>? {
        val msg = readMsg(inp) ?: return null
        if (msg.isEmpty() || (msg[0].toInt() and 0xFF) != 7) return null
        if (msg.size < 9) return null
        val buf = ByteBuffer.wrap(msg, 1, msg.size - 1)
        val piece = buf.int
        val offset = buf.int
        val data = msg.copyOfRange(9, msg.size)
        return Triple(piece, offset, data)
    }

    private fun readMsg(inp: InputStream): ByteArray? = runCatching {
        val lenBuf = ByteArray(4)
        var read = 0
        while (read < 4) {
            val n = inp.read(lenBuf, read, 4 - read)
            if (n < 0) return null
            read += n
        }
        val len = ByteBuffer.wrap(lenBuf).int
        if (len == 0) return ByteArray(0)
        if (len > 1 shl 20) return null
        val data = ByteArray(len)
        var total = 0
        while (total < len) {
            val n = inp.read(data, total, len - total)
            if (n < 0) return null
            total += n
        }
        data
    }.getOrNull()

    private fun writeToFiles(pieceIdx: Int, data: ByteArray, files: List<Pair<TorrentFile, RandomAccessFile>>) {
        val pieceStartGlobal = pieceIdx.toLong() * info.pieceLength
        files.forEach { (f, raf) ->
            val fileStart = f.startOffset
            val fileEnd = fileStart + f.length
            val writeStart = maxOf(pieceStartGlobal, fileStart)
            val writeEnd = minOf(pieceStartGlobal + data.size, fileEnd)
            if (writeStart < writeEnd) {
                val fileOffset = writeStart - fileStart
                val dataStart = (writeStart - pieceStartGlobal).toInt()
                val count = (writeEnd - writeStart).toInt()
                raf.seek(fileOffset)
                raf.write(data, dataStart, count)
            }
        }
    }

    private fun verifySha1(data: ByteArray, expected: ByteArray): Boolean =
        MessageDigest.getInstance("SHA-1").digest(data).contentEquals(expected)

    private suspend fun emitProgress(onProgress: suspend (TorrentProgress) -> Unit, status: String) {
        onProgress(buildProgress(status))
    }

    private fun buildProgress(status: String): TorrentProgress {
        val done = downloadedBytes.get()
        val speed = speedTracker.getSpeed()
        val completedPieces = pieceStates.count { it.done }
        val neededPieces = determineNeededPieces()
        val neededDone = neededPieces.count { pieceStates[it].done }
        val totalNeeded = neededPieces.size
        val left = if (totalNeeded > 0)
            (totalNeeded - neededDone).toLong() * info.pieceLength else 0L
        val eta = if (speed > 0 && left > 0) (left / speed).toInt() else -1
        val pct = if (totalNeeded > 0) neededDone.toFloat() / totalNeeded else 0f
        val filesWithSelection = info.files.mapIndexed { idx, f ->
            f.copy(selected = fileSelection.getOrElse(idx) { true })
        }
        return TorrentProgress(
            info.infoHashHex, info.name, info.totalSize, done, uploadedBytes.get(),
            speed, 0L, peers.values.toList(), peers.values.count { it.connected },
            0, 0, info.pieces.size, completedPieces, eta, pct, status, filesWithSelection
        )
    }

    fun stop() {
        stopped = true
        job?.cancel()
    }

    fun getPeers() = peers.values.toList()
}

class SpeedTracker {
    private val samples = ArrayDeque<Pair<Long, Long>>()
    private val lock = Any()

    fun add(bytes: Long) {
        val now = System.currentTimeMillis()
        synchronized(lock) {
            samples.addLast(now to bytes)
            val cutoff = now - 3000
            while (samples.isNotEmpty() && samples.first().first < cutoff) samples.removeFirst()
        }
    }

    fun getSpeed(): Long {
        synchronized(lock) {
            if (samples.size < 2) return 0
            val window = System.currentTimeMillis() - samples.first().first
            if (window <= 0) return 0
            return samples.sumOf { it.second } * 1000 / window
        }
    }
}

class BencodeDecoder(private val data: ByteArray) {
    var pos = 0

    fun decode(): Any? = when {
        pos >= data.size -> null
        data[pos] == 'i'.code.toByte() -> decodeInt()
        data[pos] == 'l'.code.toByte() -> decodeList()
        data[pos] == 'd'.code.toByte() -> decodeDict()
        data[pos].toInt().toChar().isDigit() -> decodeString()
        else -> null
    }

    fun decodeWithOffset(): Int { decode(); return pos }

    private fun decodeInt(): Long {
        pos++
        val end = data.indexOf('e'.code.toByte(), pos)
        val n = String(data, pos, end - pos).toLong()
        pos = end + 1
        return n
    }

    private fun decodeString(): ByteArray {
        val colon = data.indexOf(':'.code.toByte(), pos)
        val len = String(data, pos, colon - pos).toInt()
        pos = colon + 1
        val s = data.copyOfRange(pos, pos + len)
        pos += len
        return s
    }

    private fun decodeList(): List<Any?> {
        pos++
        val list = mutableListOf<Any?>()
        while (pos < data.size && data[pos] != 'e'.code.toByte()) list.add(decode())
        pos++
        return list
    }

    private fun decodeDict(): Map<Any?, Any?> {
        pos++
        val map = mutableMapOf<Any?, Any?>()
        while (pos < data.size && data[pos] != 'e'.code.toByte()) {
            val key = decode()
            val value = decode()
            if (key is ByteArray) map[String(key)] = value else map[key] = value
        }
        pos++
        return map
    }

    private fun ByteArray.indexOf(b: Byte, from: Int): Int {
        for (i in from until size) if (this[i] == b) return i
        return size
    }
}