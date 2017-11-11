import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.*;
import io.netty.util.CharsetUtil;

/**
 * @author Foghost
 * @since 2017/6/1
 */
public class Main1 {

  public static void main(String[] args) throws Exception {
//        Intrinsics.checkParameterIsNotNull(args, "args");
//        IO.Options options = new IO.Options();
//        options.path = "/ws";
//        options.query = "sign=ee4aaa7407c04d5fea60af8510f4b63cd2b7b57cf3b55c0e5eb570b62399f563&from=lalala123&timestamp=1501042038642";
//        options.transports = new String[]{"websocket"};
//        options.forceNew = true;
//        final Socket socket = IO.socket("http://localhost:4567/speaker", options);
//        socket.on(Socket.EVENT_CONNECT, it -> {
//            System.out.println("connected");
//            try {
//                socket.emit("msg", (new JSONObject())
//                    .put("protocol_id", "1002030")
//                    .put("from", "lalala123")
//                    .put("log_id", "sdafdasfad")
//                    .put("timestamp", 1498040779250L)
//                    .put("request", (new JSONObject())
//                        .put("rid", "dddddd")
//                        .put("body", (new JSONObject())
//                            .put("product", "H1S")
//                            .put("lang", "cn")
//                            .put("intention", "music")
//                            .put("text", "周杰伦的歌"))));
//            } catch (JSONException e) {
//                e.printStackTrace();
//            }
//        })
//            .on("msg", msg -> Stream.of(msg).forEach(o -> System.out.println(o.toString())))
//            .on(Socket.EVENT_ERROR, System.out::println)
//            .on(Socket.EVENT_CONNECT_TIMEOUT, System.out::println)
//            .on(Socket.EVENT_DISCONNECT, System.out::println)
//            .on(Socket.EVENT_CONNECT_ERROR, System.out::println);
//        socket.connect();

    Bootstrap bootstrap = new Bootstrap();
    Bootstrap b = bootstrap
      .group(new NioEventLoopGroup())
      .channel(NioSocketChannel.class)
      .option(ChannelOption.TCP_NODELAY, true)
      .handler(
        new ChannelInitializer<SocketChannel>() {
          @Override
          protected void initChannel(SocketChannel ch) throws Exception {
            ch.pipeline().addLast(new HttpClientCodec(),new HttpContentDecompressor(),

              new SimpleChannelInboundHandler<HttpObject>() {
                @Override
                protected void channelRead0(ChannelHandlerContext ctx, HttpObject msg) throws Exception {
                  if (msg instanceof HttpResponse) {
                    HttpResponse response = (HttpResponse) msg;

                    if (!response.headers().isEmpty()) {
                        response.headers().forEach(System.err::println);
                    }

                    if (HttpUtil.isTransferEncodingChunked(response)) {
                      System.err.println("CHUNKED CONTENT {");
                    } else {
                      System.err.println("CONTENT {");
                    }
                  }
                  if (msg instanceof HttpContent) {
                    HttpContent content = (HttpContent) msg;

                    System.err.print(content.content().toString(CharsetUtil.UTF_8));

                    if (content instanceof LastHttpContent) {
                      ctx.close();
                    }
                  }
                }
              });
          }
      });
    ChannelFuture future = b.connect("111.13.101.208", 80).sync();
    future.addListener(f -> {
        if (f.isSuccess()) {
          System.out.println("connection established");
        } else {
          System.out.println("failed");
          f.cause().printStackTrace();
        }
      });
    String msg = "Are you ok?";
    FullHttpRequest request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET,
      "/", Unpooled.wrappedBuffer(msg.getBytes("UTF-8")));
    request.headers().set(HttpHeaderNames.HOST, "www.baidu.com");
    request.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE);
    request.headers().set(HttpHeaderNames.CONTENT_LENGTH, request.content().readableBytes());
    future.channel().writeAndFlush(request);
    future.channel().closeFuture().sync();
  }
}