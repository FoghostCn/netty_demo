package netty;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.proxy.HttpProxyHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.SelfSignedCertificate;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.net.InetSocketAddress;

/**
 * @author Foghost
 * @since 2017/10/23
 */
public final class HttpServer {

  private static InternalLogger logger = InternalLoggerFactory.getInstance(HttpServer.class);

  private static final int PORT = 8080;
  private static final String WS_PATH = "/ws";

  public static void main(String[] args) throws Exception {
    // Configure the server.
    EventLoopGroup bossGroup = new NioEventLoopGroup(1);
    EventLoopGroup workerGroup = new NioEventLoopGroup();
    SelfSignedCertificate selfSignedCertificate = new SelfSignedCertificate();
    SslContext sslContext = SslContextBuilder.forServer(selfSignedCertificate.certificate(),
      selfSignedCertificate.privateKey()).build();
    try {
      ServerBootstrap b = new ServerBootstrap()
        .group(bossGroup, workerGroup)
        .option(ChannelOption.SO_BACKLOG, 1024)
        .childOption(ChannelOption.SO_REUSEADDR, true) //重用地址
        .childOption(ChannelOption.SO_RCVBUF, 65536)
        .childOption(ChannelOption.SO_SNDBUF, 65536)
        .childOption(ChannelOption.TCP_NODELAY, true)
        .childOption(ChannelOption.SO_KEEPALIVE, true)
        .childOption(ChannelOption.ALLOCATOR, new PooledByteBufAllocator(false))
        .childOption(ChannelOption.CONNECT_TIMEOUT_MILLIS, 1000)
        .channel(NioServerSocketChannel.class)
        .handler(new LoggingHandler(LogLevel.DEBUG))
        .childHandler(new ChannelInitializer<SocketChannel>() {
          @Override
          public void initChannel(SocketChannel ch) {
            ChannelPipeline p = ch.pipeline();
            p.addLast(new HttpProxyHandler(new InetSocketAddress("https://www.baidu.com/", 443)));

//              p.addFirst(sslContext.newHandler(ch.alloc()));
//            p.addLast(new HttpServerCodec());
//            p.addLast(new HttpContentCompressor());
//            p.addLast(new ChunkedWriteHandler());
//            p.addLast(new HttpObjectAggregator(65535));
//            p.addLast(new WebSocketServerCompressionHandler());
//            p.addLast(new IdleStateHandler(10, 0, 0));
//            p.addLast(new WebSocketServerProtocolHandler(WS_PATH, null, true));
//            p.addLast(new WebsocketHandler());
            p.addLast(new HttpServerHandler());
          }
        });

      ChannelFuture cf = b.bind(PORT).sync();

      logger.info("Open your web browser and navigate to http://127.0.0.1:" + PORT + '/');
      cf.channel().closeFuture().sync();
    } finally {
      bossGroup.shutdownGracefully();
      workerGroup.shutdownGracefully();
    }
  }
}