/*
 * Copyright 2012 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package io.netty.example.echo;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.SelfSignedCertificate;

/**
 * Echoes back any received data from a client.
 */
public final class EchoServer {

    static final boolean SSL = System.getProperty("ssl") != null;
    static final int PORT = Integer.parseInt(System.getProperty("port", "8007"));

    public static void main(String[] args) throws Exception {
        // Configure SSL.
        //配置SSL
        final SslContext sslCtx;
        if (SSL) {
            SelfSignedCertificate ssc = new SelfSignedCertificate();
            sslCtx = SslContextBuilder.forServer(ssc.certificate(), ssc.privateKey()).build();
        } else {
            sslCtx = null;
        }

        // Configure the server.
        //boss线程组，用于服务端接收客户端的连接
        //worker线程组，用于进行客户端的SocketChannel的数据读写
        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        //创建EchoServerHandler对象
        final EchoServerHandler serverHandler = new EchoServerHandler();
        try {
            //创建ServerBootstrap对象
            //启动器，能够帮助netty使用者更加方便组装和配置netty，可以更方便启动netty应用程序。
            //通过他来连接到一个主机和端口，也可以绑定到一个本地端口。
            ServerBootstrap b = new ServerBootstrap();
            /**设置使用的EventLoopGroup*/
            b.group(bossGroup, workerGroup)
                    //设置要被实例化为NIOServerSocketChannel类
             .channel(NioServerSocketChannel.class)
                    //设置NioServerSocketChannel的可选项
             .option(ChannelOption.SO_BACKLOG, 100)
                    //设置NioServerSocketChannel的处理器
             .handler(new LoggingHandler(LogLevel.INFO))
             .childHandler(new ChannelInitializer<SocketChannel>() {
                 @Override
                 public void initChannel(SocketChannel ch) throws Exception {
                     ChannelPipeline p = ch.pipeline();
                     if (sslCtx != null) {
                         p.addLast(sslCtx.newHandler(ch.alloc()));
                     }
                     //p.addLast(new LoggingHandler(LogLevel.INFO));
                     p.addLast(serverHandler);
                 }
             });

            /**先调用bind()绑定宽口，然后调用sync()方法阻塞等待成功，这个过程就是"启动服务端"*/
            // Start the server.
            //绑定端口，并同步等待成功，即启动服务端。
            ChannelFuture f = b.bind(PORT).sync();

            /**这里是监听关闭*/
            // Wait until the server socket is closed.
            //监听服务端关闭，并阻塞等待
            f.channel().closeFuture().sync();
        } finally {
            /**执行到这里，说明服务端已经关闭了，所以调用shutdownGracefully优雅关闭*/
            // Shut down all event loops to terminate all threads.
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }
}
