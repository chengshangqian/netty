/*
 * Copyright 2016 The Netty Project
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
package io.netty.util;

/**
 * 相当于一个int值结果的提供者/供应商接口
 *
 * represent 代表，等于
 *
 * Represents a supplier of {@code int}-valued results.
 */
public interface IntSupplier {

    /**
     * 获取一个int类型值的结果
     *
     * Gets a result.
     *
     * @return a result
     */
    int get() throws Exception;
}
