package com.had.downloader.service

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.Build
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.io.*
import java.net.Inet4Address
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "RemoteDownloadServer"
const val REMOTE_SERVER_PORT = 8080

data class RemoteServerState(
    val isRunning: Boolean = false,
    val ipAddress: String = "",
    val port: Int = REMOTE_SERVER_PORT,
    val requestCount: Int = 0,
    val lastRequest: String = "",
    val connectedClients: Int = 0
)

data class RemoteRequest(
    val url: String,
    val filename: String?,
    val timestamp: Long = System.currentTimeMillis(),
    val clientIp: String = ""
)

@Singleton
class RemoteDownloadServer @Inject constructor() {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var serverSocket: ServerSocket? = null
    private var serverJob: Job? = null

    private val _state = MutableStateFlow(RemoteServerState())
    val state: StateFlow<RemoteServerState> = _state

    private val connectedClients = mutableListOf<Socket>()
    private val clientsLock = Any()

    var onDownloadRequested: ((url: String, filename: String?, headers: Map<String, String>) -> Unit)? = null

    private val requestHistory = mutableListOf<RemoteRequest>()

    fun start(context: Context) {
        if (_state.value.isRunning) return

        val ip = getLocalIpAddress(context)
        if (ip == "Not connected" || ip.isBlank()) {
            Log.e(TAG, "Cannot start server: No network connection")
            _state.value = RemoteServerState(
                isRunning = false,
                ipAddress = "Not connected",
                port = REMOTE_SERVER_PORT
            )
            return
        }

        serverJob = scope.launch {
            runCatching {
                serverSocket = ServerSocket(REMOTE_SERVER_PORT)
                _state.value = RemoteServerState(
                    isRunning = true,
                    ipAddress = ip,
                    port = REMOTE_SERVER_PORT,
                    connectedClients = 0
                )
                Log.d(TAG, "Remote server started on $ip:$REMOTE_SERVER_PORT")

                while (isActive) {
                    val client = serverSocket?.accept() ?: break
                    synchronized(clientsLock) {
                        connectedClients.add(client)
                        _state.value = _state.value.copy(
                            connectedClients = connectedClients.size
                        )
                    }
                    launch { handleClient(client) }
                }
            }.onFailure { e ->
                Log.e(TAG, "Server error: ${e.message}")
                synchronized(clientsLock) {
                    connectedClients.clear()
                    _state.value = _state.value.copy(
                        isRunning = false,
                        connectedClients = 0
                    )
                }
            }
        }
    }

    fun stop() {
        serverJob?.cancel()
        synchronized(clientsLock) {
            connectedClients.forEach { runCatching { it.close() } }
            connectedClients.clear()
        }
        runCatching { serverSocket?.close() }
        serverSocket = null
        _state.value = RemoteServerState(
            isRunning = false,
            ipAddress = "",
            port = REMOTE_SERVER_PORT,
            connectedClients = 0
        )
        Log.d(TAG, "Remote server stopped")
    }

    private fun handleClient(socket: Socket) {
        runCatching {
            socket.soTimeout = 10000
            val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
            val writer = PrintWriter(BufferedWriter(OutputStreamWriter(socket.getOutputStream())), true)

            val requestLine = reader.readLine() ?: return
            val parts = requestLine.split(" ")
            if (parts.size < 2) return

            val method = parts[0]
            val path = parts[1]

            val headers = mutableMapOf<String, String>()
            var line = reader.readLine()
            while (!line.isNullOrBlank()) {
                val idx = line.indexOf(':')
                if (idx > 0) {
                    headers[line.substring(0, idx).trim().lowercase()] = line.substring(idx + 1).trim()
                }
                line = reader.readLine()
            }

            val body = if (method == "POST") {
                val contentLength = headers["content-length"]?.toIntOrNull() ?: 0
                if (contentLength > 0) {
                    val cbuf = CharArray(contentLength)
                    reader.read(cbuf, 0, contentLength)
                    String(cbuf)
                } else ""
            } else ""

            val clientIp = socket.inetAddress.hostAddress ?: "unknown"

            when {
                path == "/" || path == "/index.html" -> sendHtmlPage(writer, clientIp)
                path == "/api/download" && method == "POST" -> handleDownloadApi(writer, body, clientIp)
                path == "/api/history" -> sendHistory(writer)
                path == "/api/status" -> sendStatus(writer)
                else -> sendNotFound(writer)
            }

            writer.flush()
        }.onFailure { e ->
            Log.w(TAG, "Client error: ${e.message}")
        }

        synchronized(clientsLock) {
            connectedClients.remove(socket)
            _state.value = _state.value.copy(
                connectedClients = connectedClients.size
            )
        }
        runCatching { socket.close() }
    }

    private fun handleDownloadApi(writer: PrintWriter, body: String, clientIp: String) {
        runCatching {
            val json = JSONObject(body)
            val url = json.optString("url", "").trim()
            val filename = json.optString("filename", "").ifBlank { null }
            val queue = json.optBoolean("queue", false)

            val headersJson = json.optJSONObject("headers")
            val headers = mutableMapOf<String, String>()
            headersJson?.keys()?.forEach { k ->
                headers[k] = headersJson.getString(k)
            }

            if (url.isBlank() || !url.startsWith("http")) {
                sendJsonResponse(writer, 400, """{"success":false,"error":"Invalid URL"}""")
                return
            }

            val request = RemoteRequest(url, filename, clientIp = clientIp)
            requestHistory.add(0, request)
            if (requestHistory.size > 50) {
                requestHistory.removeAt(requestHistory.size - 1)
            }

            _state.value = _state.value.copy(
                requestCount = _state.value.requestCount + 1,
                lastRequest = url
            )

            onDownloadRequested?.invoke(url, filename, headers)

            val message = if (queue) "Added to queue" else "Download started"
            sendJsonResponse(
                writer,
                200,
                """{"success":true,"message":"$message","url":"$url","queued":$queue}"""
            )
        }.onFailure { e ->
            sendJsonResponse(writer, 500, """{"success":false,"error":"${e.message}"}""")
        }
    }

    private fun sendHistory(writer: PrintWriter) {
        val arr = JSONArray()
        requestHistory.forEach { req ->
            arr.put(JSONObject().apply {
                put("url", req.url)
                put("filename", req.filename ?: "")
                put("timestamp", req.timestamp)
                put("clientIp", req.clientIp)
            })
        }
        sendJsonResponse(writer, 200, arr.toString())
    }

    private fun sendStatus(writer: PrintWriter) {
        val s = _state.value
        val json = JSONObject().apply {
            put("running", s.isRunning)
            put("ip", s.ipAddress)
            put("port", s.port)
            put("requestCount", s.requestCount)
            put("connectedClients", s.connectedClients)
            put("lastRequest", s.lastRequest)
        }
        sendJsonResponse(writer, 200, json.toString())
    }

    private fun sendJsonResponse(writer: PrintWriter, code: Int, body: String) {
        val status = when (code) {
            200 -> "200 OK"
            400 -> "400 Bad Request"
            404 -> "404 Not Found"
            else -> "500 Internal Server Error"
        }
        writer.print("HTTP/1.1 $status\r\n")
        writer.print("Content-Type: application/json\r\n")
        writer.print("Access-Control-Allow-Origin: *\r\n")
        writer.print("Content-Length: ${body.toByteArray().size}\r\n")
        writer.print("Connection: close\r\n")
        writer.print("\r\n")
        writer.print(body)
    }

    private fun sendNotFound(writer: PrintWriter) {
        val body = """{"error":"Not found"}"""
        writer.print("HTTP/1.1 404 Not Found\r\n")
        writer.print("Content-Type: application/json\r\n")
        writer.print("Content-Length: ${body.length}\r\n")
        writer.print("Connection: close\r\n")
        writer.print("\r\n")
        writer.print(body)
    }

    private fun sendHtmlPage(writer: PrintWriter, clientIp: String) {
        val html = buildRemoteWebPage()
        writer.print("HTTP/1.1 200 OK\r\n")
        writer.print("Content-Type: text/html; charset=UTF-8\r\n")
        writer.print("Content-Length: ${html.toByteArray().size}\r\n")
        writer.print("Connection: close\r\n")
        writer.print("\r\n")
        writer.print(html)
    }

    private fun buildRemoteWebPage(): String {
        val ip = _state.value.ipAddress
        val port = _state.value.port

        return """
<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<title>HAD Remote Download</title>
<style>
  :root {
    --bg: #080B14; --surface: #0F1420; --elevated: #161C2E;
    --border: #1E2640; --cyan: #00D4FF; --green: #00FF88;
    --red: #FF4057; --orange: #FF8C42; --purple: #9B59FF;
    --text: #E8EDF5; --muted: #6B7A99; --dim: #3A4460;
  }
  * { box-sizing: border-box; margin: 0; padding: 0; }
  body { background: var(--bg); color: var(--text); font-family: 'Segoe UI', monospace; min-height: 100vh; padding: 20px; }
  .header { text-align: center; padding: 40px 20px 30px; }
  .logo { font-size: 48px; font-weight: 900; color: var(--cyan); letter-spacing: 8px; text-shadow: 0 0 30px rgba(0,212,255,0.5); }
  .subtitle { color: var(--muted); font-size: 13px; letter-spacing: 3px; margin-top: 8px; }
  .card { background: var(--surface); border: 1px solid var(--border); border-radius: 16px; padding: 24px; margin: 16px auto; max-width: 640px; }
  .card-title { color: var(--muted); font-size: 10px; letter-spacing: 3px; font-weight: 700; margin-bottom: 16px; }
  .input-group { margin-bottom: 14px; }
  label { display: block; color: var(--muted); font-size: 12px; margin-bottom: 6px; }
  input[type=text], input[type=url], textarea {
    width: 100%; background: var(--elevated); border: 1px solid var(--border);
    border-radius: 10px; padding: 12px 14px; color: var(--text); font-size: 13px;
    font-family: monospace; outline: none; transition: border-color 0.2s;
  }
  input:focus, textarea:focus { border-color: var(--cyan); }
  .btn { display: inline-flex; align-items: center; gap: 8px; padding: 12px 24px;
    border-radius: 10px; border: none; cursor: pointer; font-weight: 700;
    font-size: 14px; transition: all 0.2s; }
  .btn-primary { background: var(--cyan); color: #080B14; }
  .btn-primary:hover { box-shadow: 0 0 20px rgba(0,212,255,0.4); transform: translateY(-1px); }
  .btn-secondary { background: transparent; color: var(--purple); border: 1px solid rgba(155,89,255,0.5); }
  .btn-danger { background: transparent; color: var(--red); border: 1px solid rgba(255,64,87,0.5); }
  .btn-row { display: flex; gap: 10px; flex-wrap: wrap; }
  .status { display: flex; align-items: center; gap: 8px; padding: 10px 14px;
    background: rgba(0,255,136,0.07); border: 1px solid rgba(0,255,136,0.2);
    border-radius: 10px; margin-bottom: 16px; }
  .dot { width: 8px; height: 8px; border-radius: 50%; background: var(--green);
    animation: pulse 1.5s ease-in-out infinite; }
  @keyframes pulse { 0%,100%{opacity:0.4} 50%{opacity:1} }
  .status-text { color: var(--green); font-size: 12px; font-family: monospace; }
  .status-text.offline { color: var(--red); }
  .history-item { display: flex; align-items: center; justify-content: space-between;
    padding: 10px 12px; background: var(--elevated); border-radius: 8px;
    margin-bottom: 6px; border: 1px solid var(--border); }
  .history-url { color: var(--muted); font-size: 11px; font-family: monospace;
    overflow: hidden; text-overflow: ellipsis; white-space: nowrap; flex: 1; }
  .history-time { color: var(--dim); font-size: 9px; font-family: monospace; margin-left: 8px; }
  .toast { position: fixed; bottom: 24px; right: 24px; padding: 12px 20px;
    background: var(--surface); border: 1px solid var(--cyan); border-radius: 10px;
    color: var(--cyan); font-size: 13px; transform: translateY(80px);
    transition: transform 0.3s; z-index: 100; }
  .toast.show { transform: translateY(0); }
  .toast.error { border-color: var(--red); color: var(--red); }
  #counter { color: var(--cyan); font-weight: 700; }
  .info-row { display: flex; gap: 16px; color: var(--muted); font-size: 11px; font-family: monospace; flex-wrap: wrap; }
  .info-row span { background: var(--elevated); padding: 4px 10px; border-radius: 6px; border: 1px solid var(--border); }
</style>
</head>
<body>
<div class="header">
  <div class="logo">HAD</div>
  <div class="subtitle">HYPER ADVANCED DOWNLOADER · REMOTE</div>
</div>
<div class="card">
  <div class="status">
    <div class="dot" id="statusDot"></div>
    <span class="status-text" id="statusText">Server online</span>
  </div>
  <div class="info-row" id="infoRow">
    <span>IP: <b id="ipDisplay">$ip</b></span>
    <span>Port: <b>$port</b></span>
    <span>Requests: <b id="counter">0</b></span>
    <span>Clients: <b id="clientCount">0</b></span>
  </div>
</div>
<div class="card">
  <div class="card-title">DOWNLOAD A FILE</div>
  <div class="input-group">
    <label>URL</label>
    <input type="url" id="urlInput" placeholder="https://example.com/file.mp4">
  </div>
  <div class="input-group">
    <label>Filename (optional)</label>
    <input type="text" id="filenameInput" placeholder="leave blank to auto-detect">
  </div>
  <div class="btn-row">
    <button class="btn btn-primary" onclick="sendDownload(false)">Download Now</button>
    <button class="btn btn-secondary" onclick="sendDownload(true)">Add to Queue</button>
  </div>
</div>
<div class="card">
  <div class="card-title">BULK DOWNLOAD</div>
  <div class="input-group">
    <label>Multiple URLs (one per line)</label>
    <textarea id="bulkInput" rows="5" placeholder="https://..."></textarea>
  </div>
  <div class="btn-row">
    <button class="btn btn-primary" onclick="sendBulk(false)">Download All</button>
    <button class="btn btn-secondary" onclick="sendBulk(true)">Queue All</button>
  </div>
</div>
<div class="card">
  <div class="card-title">RECENT REQUESTS</div>
  <div id="history"><div style="color:var(--dim);font-size:12px;text-align:center;padding:16px">No requests yet</div></div>
</div>
<div class="toast" id="toast"></div>
<script>
let requestCount = 0;

async function sendDownload(queue) {
  const url = document.getElementById('urlInput').value.trim();
  if (!url) { showToast('Please enter a URL', true); return; }
  const filename = document.getElementById('filenameInput').value.trim();
  try {
    const r = await fetch('/api/download', {
      method: 'POST',
      headers: {'Content-Type':'application/json'},
      body: JSON.stringify({url: url, filename: filename || null, queue: queue})
    });
    const j = await r.json();
    if (j.success) {
      requestCount++;
      document.getElementById('counter').textContent = requestCount;
      showToast(j.message);
      document.getElementById('urlInput').value = '';
      loadHistory();
    } else {
      showToast('Error: ' + j.error, true);
    }
  } catch(e) {
    showToast('Connection error', true);
  }
}

async function sendBulk(queue) {
  const urls = document.getElementById('bulkInput').value.split('\\n')
    .map(u => u.trim()).filter(u => u.startsWith('http'));
  if (!urls.length) { showToast('No valid URLs', true); return; }
  let count = 0;
  for (const u of urls) {
    try {
      const r = await fetch('/api/download', {
        method: 'POST',
        headers: {'Content-Type':'application/json'},
        body: JSON.stringify({url: u, filename: null, queue: queue})
      });
      const j = await r.json();
      if (j.success) { count++; }
    } catch(e) {}
  }
  showToast('Queued ' + count + ' of ' + urls.length + ' URLs');
  document.getElementById('bulkInput').value = '';
  loadHistory();
}

async function loadHistory() {
  try {
    const r = await fetch('/api/history');
    const data = await r.json();
    const el = document.getElementById('history');
    if (!data.length) {
      el.innerHTML = '<div style="color:var(--dim);font-size:12px;text-align:center;padding:16px">No requests yet</div>';
      return;
    }
    el.innerHTML = data.slice(0,20).map(item => {
      const name = item.url.split('/').pop().split('?')[0].substring(0,40) || item.url.substring(0,40);
      const time = new Date(item.timestamp).toLocaleTimeString();
      return '<div class="history-item">' +
        '<span class="history-url" title="'+item.url+'">'+name+'</span>' +
        '<span class="history-time">'+time+'</span>' +
        '<button onclick="redownload(\''+item.url+'\')" style="background:rgba(0,212,255,0.1);border:1px solid rgba(0,212,255,0.3);color:var(--cyan);border-radius:6px;padding:3px 8px;cursor:pointer;font-size:10px;margin-left:8px">↓</button>' +
        '</div>';
    }).join('');
  } catch(e) {}
}

async function redownload(url) {
  document.getElementById('urlInput').value = url;
  sendDownload(false);
}

async function updateStatus() {
  try {
    const r = await fetch('/api/status');
    const s = await r.json();
    document.getElementById('counter').textContent = s.requestCount;
    document.getElementById('clientCount').textContent = s.connectedClients || 0;
    const dot = document.getElementById('statusDot');
    const text = document.getElementById('statusText');
    if (s.running) {
      dot.style.background = 'var(--green)';
      text.textContent = 'Server online · ' + s.ip + ':' + s.port;
      text.className = 'status-text';
    } else {
      dot.style.background = 'var(--red)';
      text.textContent = 'Server offline';
      text.className = 'status-text offline';
    }
  } catch(e) {}
}

function showToast(msg, error) {
  const t = document.getElementById('toast');
  t.textContent = msg;
  t.className = 'toast' + (error ? ' error' : '');
  t.classList.add('show');
  setTimeout(() => t.classList.remove('show'), 3000);
}

loadHistory();
updateStatus();
setInterval(loadHistory, 5000);
setInterval(updateStatus, 3000);
</script>
</body>
</html>
        """.trimIndent()
    }

    private fun getLocalIpAddress(context: Context): String {
        runCatching {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val network = connectivityManager?.activeNetwork
                val capabilities = connectivityManager?.getNetworkCapabilities(network)
                if (capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true ||
                    capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) == true
                ) {
                }
            }
        }

        runCatching {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val intf = interfaces.nextElement()
                if (!intf.isUp || intf.isLoopback || intf.isVirtual) continue

                val name = intf.name.lowercase()
                if (name.startsWith("wlan") || name.startsWith("eth") || name.startsWith("en")) {
                    val addrs = intf.inetAddresses
                    while (addrs.hasMoreElements()) {
                        val addr = addrs.nextElement()
                        if (!addr.isLoopbackAddress && addr is Inet4Address) {
                            val ip = addr.hostAddress
                            if (!ip.isNullOrBlank() && ip != "0.0.0.0" && !ip.startsWith("169.254")) {
                                Log.d(TAG, "Found IP via NetworkInterface: $ip")
                                return ip
                            }
                        }
                    }
                }
            }
        }

        runCatching {
            @Suppress("DEPRECATION")
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            val ipInt = wifiManager?.connectionInfo?.ipAddress
            if (ipInt != null && ipInt != 0) {
                val ip = String.format(
                    "%d.%d.%d.%d",
                    ipInt and 0xff,
                    ipInt shr 8 and 0xff,
                    ipInt shr 16 and 0xff,
                    ipInt shr 24 and 0xff
                )
                if (!ip.startsWith("169.254") && ip != "0.0.0.0") {
                    Log.d(TAG, "Found IP via WifiManager: $ip")
                    return ip
                }
            }
        }

        runCatching {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val intf = interfaces.nextElement()
                if (!intf.isUp || intf.isLoopback || intf.isVirtual) continue
                val addrs = intf.inetAddresses
                while (addrs.hasMoreElements()) {
                    val addr = addrs.nextElement()
                    if (!addr.isLoopbackAddress && addr is Inet4Address) {
                        val ip = addr.hostAddress
                        if (!ip.isNullOrBlank() && ip != "0.0.0.0" && !ip.startsWith("169.254")) {
                            Log.d(TAG, "Found fallback IP: $ip")
                            return ip
                        }
                    }
                }
            }
        }

        return "Not connected"
    }
}