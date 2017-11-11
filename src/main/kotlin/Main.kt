import io.socket.client.IO
import io.socket.client.Socket
import org.json.JSONObject


class Main

fun main(args: Array<String>) {
    val options = IO.Options()
    options.path = "/ws"
    options.query = "sign=ee4aaa7407c04d5fea60af8510f4b63cd2b7b57cf3b55c0e5eb570b62399f563&from=lalala123&timestamp=1501042038642"
    options.transports = arrayOf("websocket")
    options.forceNew = true
    val socket = IO.socket("http://localhost:4567/speaker", options)
    socket.on(Socket.EVENT_CONNECT_ERROR, {
        err ->
        println(err)
    }).on("msg", {
        msg ->
        run {
            for (m in msg) {
                if (m is JSONObject) {
                    println(m.toString())
                }
            }
        }
    }).on(Socket.EVENT_ERROR, {
        err ->
        println(err)
    }).on(Socket.EVENT_CONNECT_TIMEOUT, {
        err ->
        println(err)
    }).on(Socket.EVENT_DISCONNECT, {
        err ->
        run {
            println(err)
            println("disconnect")
        }
    }).on(Socket.EVENT_CONNECT, {
        println("connected")
        socket.emit("msg", JSONObject()
            .put("protocol_id", "1002030")
            .put("from", "lalala123")
            .put("log_id", "sdafdasfad")
            .put("timestamp", 1498040779250)
            .put("request", JSONObject()
                .put("rid", "dddddd")
                .put("body", JSONObject()
                    .put("product", "H1S")
                    .put("lang", "cn")
                    .put("intention", "music")
                    .put("text", "周杰伦的歌")
                )
            )
        )
    })
    socket.connect()
}
