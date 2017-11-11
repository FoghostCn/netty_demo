import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.util.CharsetUtil;

/**
 * @author Foghost
 * @since 2017/10/23
 */
public class HttpHelloWorldServerHandler extends SimpleChannelInboundHandler<FullHttpRequest> {
	private static final byte[] CONTENT = {'H', 'e', 'l', 'l', 'o', ' ', 'W', 'o', 'r', 'l', 'd'};

	@Override
	public void channelReadComplete(ChannelHandlerContext ctx) {
		ctx.flush();

	}

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest request) throws Exception {
			ctx.fireChannelRead(request.retain());
			request.headers().forEach(System.out::println);
			System.out.println();
			System.out.println(request.content().toString(CharsetUtil.UTF_8));
			ctx.close();
//		if (HttpUtil.is100ContinueExpected(req)) {
//			ctx.write(new DefaultFullHttpResponse(HTTP_1_1, CONTINUE));
//		}
//		boolean keepAlive = HttpUtil.isKeepAlive(req);
//		FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, OK, Unpooled.wrappedBuffer(CONTENT));
//		response.headers().set(CONTENT_TYPE, "text/plain");
//		response.headers().set(CONTENT_LENGTH, response.content().readableBytes());
//
//		if (!keepAlive) {
//			ctx.write(response).addListener(ChannelFutureListener.CLOSE);
//		} else {
//			response.headers().set(CONNECTION, HttpHeaderValues.KEEP_ALIVE);
//			ctx.write(response);
//		}
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
		cause.printStackTrace();
		ctx.close();
	}
}
