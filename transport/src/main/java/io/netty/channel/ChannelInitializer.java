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
package io.netty.channel;

import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 通道初始化器
 * 一个专门设计的{@link ChannelInboundHandler}抽象实现类，当{@link Channel}被注册到它相关的事件循环{@link EventLoop}后，提供简单易用的方式初始化一个通道{@link Channel}
 *
 * special 专门的，特殊的，特别的
 * initialize 初始化
 * once 一旦，当…时候
 *
 * A special {@link ChannelInboundHandler} which offers an easy way to initialize a {@link Channel} once it was
 * registered to its {@link EventLoop}.
 *
 * {@link ChannelInitializer}的实现类大多用在{@link Bootstrap#handler(ChannelHandler)}、{@link ServerBootstrap#handler(ChannelHandler)}、{@link ServerBootstrap#childHandler(ChannelHandler)}
 * 等设置一个通道{@link Channel}的通道管道{@link ChannelPipeline}的上下文代码中。
 *
 * most often 时常;常常;往往;大多
 *
 * Implementations are most often used in the context of {@link Bootstrap#handler(ChannelHandler)} ,
 * {@link ServerBootstrap#handler(ChannelHandler)} and {@link ServerBootstrap#childHandler(ChannelHandler)} to
 * setup the {@link ChannelPipeline} of a {@link Channel}.
 *
 * <pre>
 *
 * public class MyChannelInitializer extends {@link ChannelInitializer} {
 *     public void initChannel({@link Channel} channel) {
 *         channel.pipeline().addLast("myHandler", new MyHandler());
 *     }
 * }
 *
 * {@link ServerBootstrap} bootstrap = ...;
 * ...
 * bootstrap.childHandler(new MyChannelInitializer());
 * ...
 * </pre>
 *
 * 注意，这个ChannelInitializer抽象类被标记/注解为@Sharable，所以它的子类必须对复用是安全的
 *
 * Be aware that this class is marked as {@link Sharable} and so the implementation must be safe to be re-used.
 *
 * @param <C>   A sub-type of {@link Channel}
 */
@Sharable
public abstract class ChannelInitializer<C extends Channel> extends ChannelInboundHandlerAdapter {

    /**
     * 内部日志
     */
    private static final InternalLogger logger = InternalLoggerFactory.getInstance(ChannelInitializer.class);

    // 通道处理器上下文初始映射集
    // 我们使用一个集合Set作为通道初始化器ChannelInitializer，它通常在Bootstrap/ServerBootstrap中的所有通道之间共享。与使用属性Attributes相比，这样可以减少内存使用。
    //
    // reduce 减少
    //
    // We use a Set as a ChannelInitializer is usually shared between all Channels in a Bootstrap /
    // ServerBootstrap. This way we can reduce the memory usage compared to use Attributes.
    private final Set<ChannelHandlerContext> initMap = Collections.newSetFromMap(
            new ConcurrentHashMap<ChannelHandlerContext, Boolean>());

    /**
     * 初始化通道
     * 由业务代码去实现，一般会在这个方法中，获取通道管道，然后将实现业务而编写的通道处理器添加到通道管道中。
     * 这个方法在通道{@link Channel}被注册(到EventLoop)后调用。
     * 在方法返回后，这个实例（指{@link ChannelInitializer}，见{@link #initChannel(ChannelHandlerContext)}）实例会从该通道 {@link Channel}的通道管道{@link ChannelPipeline}上移除
     * This method will be called once the {@link Channel} was registered. After the method returns this instance
     * will be removed from the {@link ChannelPipeline} of the {@link Channel}.
     *
     * @param ch            the {@link Channel} which was registered. 被注册的通道{@link Channel}
     * @throws Exception    is thrown if an error occurs. In that case it will be handled by
     *                      {@link #exceptionCaught(ChannelHandlerContext, Throwable)} which will by default close
     *                      the {@link Channel}.
     */
    protected abstract void initChannel(C ch) throws Exception;

    /**
     * 通道注册完成事件
     * 对于通道初始化器，基本上不会收到通道注册完成事件，所以次方法基本上不会被调用。
     * 因为在注册通道后，首先，Netty会先触发handlerAdded事件，先调用初始化器的{@link #handlerAdded(ChannelHandlerContext)}方法,
     * 而{@link #handlerAdded(ChannelHandlerContext)}方法会在调用完{@link #initChannel(ChannelHandlerContext)}方法后，
     * 会将通道初始化器从pipeline中移除。接着，Netty才会触发channelRegistered事件，但此时通道初始化已经从pipeline中移除，无法接收到事件，
     * 所以此方法永远不会被调用。仅供深度定制参考。
     *
     * 注册后相关事件的触发流程见{@link AbstractChannel}的内部类AbstractUnsafe的register0(ChannelPromise)方法
     *
     * @param ctx 通道处理器上下文
     * @throws Exception
     */
     @Override
    @SuppressWarnings("unchecked")
    public final void channelRegistered(ChannelHandlerContext ctx) throws Exception {
         // 一般情况下，这个方法永远不会被调用，因为handlerAdded(ctx)方法会调用initChannel(ctx)方法然后删除对应的处理器handler
        // Normally this method will never be called as handlerAdded(...) should call initChannel(...) and remove
        // the   handler.

         // 初始化通道
        if (initChannel(ctx)) {
            // 我们调用完initChannel(...)后，需要调用pipeline.fireChannelRegistered()以确保不会丢失/错过事件：传递给下一个上下文?
            // we called initChannel(...) so we need to call now pipeline.fireChannelRegistered() to ensure we not
            // miss an event.

            // 从管道中的第一个上下文即上下文头开始触发通道注册事件
            ctx.pipeline().fireChannelRegistered();

            // 通道初始化完毕，移除通道的所有状态
            // We are done with init the Channel, removing all the state for the Channel now.
            removeState(ctx);
        } else {
            // Called initChannel(...) before which is the expected behavior, so just forward the event.
            // 将通道注册成功事件传递给下一个处理器
            // 从当前上下文开始触发通道注册事件，然后传递给下一个
            ctx.fireChannelRegistered();
        }
    }

    /**
     * Handle the {@link Throwable} by logging and closing the {@link Channel}. Sub-classes may override this.
     */
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        if (logger.isWarnEnabled()) {
            logger.warn("Failed to initialize a channel. Closing: " + ctx.channel(), cause);
        }
        ctx.close();
    }

    /**
     * 处理器添加事件
     * 调用pipeline的addXXX系列方法添加handler时触发
     *
     * 如果子类要重载此方法，请确保调用父类的super方法
     *
     * {@inheritDoc} If override this method ensure you call super!
     */
    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
        logger.debug("================> 收到handlerAdded事件...");
        // 如果关联的通道channel已注册
        if (ctx.channel().isRegistered()) {
            // This should always be true with our current DefaultChannelPipeline implementation.
            // The good thing about calling initChannel(...) in handlerAdded(...) is that there will be no ordering
            // surprises if a ChannelInitializer will add another ChannelInitializer. This is as all handlers
            // will be added in the expected order.
            // 对于当前缺省通道管道实现，注册状态一般应该都是true。
            // 在handlerAdded(...)方法中调用initChannel(...)是件好事，即使一个通道初始化器将添加另外一个通道初始化器它也不会出现顺序意外（即能保持添加的顺序）。
            // 这是因为所有的处理器都会按预期的按顺序被添加

            // 调用处理器的初始化通道方法
            logger.debug("================> handlerAdded中调用处理器的初始化通道方法initChannel(ctx)...");
            if (initChannel(ctx)) {

                // 通道初始化完毕，移除通道初始化器，避免重复触发此事件，重复添加处理器
                // We are done with init the Channel, removing the initializer now.
                removeState(ctx);
            }
        }
    }

    /**
     * 删除上下文
     *
     * @param ctx
     * @throws Exception
     */
    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
        initMap.remove(ctx);
    }

    /**
     * 初始化通道，将开发人员编写的业务逻辑处理器添加到pipeline中
     * 当发生handlerAdded事件或channelRegistered事件时将会被调用（事实上channelRegistered事件在初始化器中永远不会被触发）。
     *
     * @param ctx 通道处理器上下文
     * @return
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    private boolean initChannel(ChannelHandlerContext ctx) throws Exception {
        // 【首次】将通道处理器上下文ctx添加到initMap中
        if (initMap.add(ctx)) { // Guard against re-entrance.
            logger.debug("================> 调用initMap.add(ctx)...");
            try {
                // 抽象模板方法，最终调用具体子类的实现，初始化通道，为注册的通道添加通道处理器
                logger.debug("================> 调用initChannel((C) ctx.channel())...");
                initChannel((C) ctx.channel());
            } catch (Throwable cause) {
                // Explicitly call exceptionCaught(...) as we removed the handler before calling initChannel(...).
                // We do so to prevent multiple calls to initChannel(...).
                exceptionCaught(ctx, cause);
            } finally {
                // 将通道初始化器自身从pipeline中删除，避免下次重复触发事件
                ChannelPipeline pipeline = ctx.pipeline();
                if (pipeline.context(this) != null) {
                    pipeline.remove(this);
                }
            }
            return true;
        }
        return false;
    }

    /**
     * 删除上下文
     *
     * @param ctx
     */
    private void removeState(final ChannelHandlerContext ctx) {
        // The removal may happen in an async fashion if the EventExecutor we use does something funky.
        if (ctx.isRemoved()) {
            initMap.remove(ctx);
        } else {
            // The context is not removed yet which is most likely the case because a custom EventExecutor is used.
            // Let's schedule it on the EventExecutor to give it some more time to be completed in case it is offloaded.
            ctx.executor().execute(new Runnable() {
                @Override
                public void run() {
                    initMap.remove(ctx);
                }
            });
        }
    }
}
