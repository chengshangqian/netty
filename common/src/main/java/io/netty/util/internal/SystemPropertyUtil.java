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
package io.netty.util.internal;

import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.security.AccessController;
import java.security.PrivilegedAction;

/**
 * 系统属性实用类，检索和解析Java系统属性值的使用方法集和
 *
 * A collection of utility methods to retrieve and parse the values of the Java system properties.
 */
public final class SystemPropertyUtil {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(SystemPropertyUtil.class);

    /**
     * 是否包含指定key的系统属性值
     *
     * Returns {@code true} if and only if the system property with the specified {@code key}
     * exists.
     */
    public static boolean contains(String key) {
        return get(key) != null;
    }

    /**
     * 返回指定key对应的Java系统属性值，如果属性访问失败，则返回null
     *
     * Returns the value of the Java system property with the specified
     * {@code key}, while falling back to {@code null} if the property access fails.
     *
     * @return the property value or {@code null} 返回属性值或null
     */
    public static String get(String key) {
        return get(key, null);
    }

    /**
     * 返回指定key对应的Java系统属性值，如果属性访问失败，则返回指定的默认值def
     *
     * Returns the value of the Java system property with the specified
     * {@code key}, while falling back to the specified default value if
     * the property access fails.
     *
     * @return the property value.
     *         {@code def} if there's no such property or if an access to the
     *         specified property is not allowed.
     */
    public static String get(final String key, String def) {
        // null检查，null将抛出空指针NullPointerException异常
        ObjectUtil.checkNotNull(key, "key");

        // 空检查，抛出IllegalArgumentException异常
        if (key.isEmpty()) {
            throw new IllegalArgumentException("key must not be empty.");
        }

        // 属性值
        String value = null;
        try {
            // 获取系统属性值，可能有安全访问权限限制
            if (System.getSecurityManager() == null) {
                value = System.getProperty(key);
            } else {
                value = AccessController.doPrivileged(new PrivilegedAction<String>() {
                    @Override
                    public String run() {
                        return System.getProperty(key);
                    }
                });
            }
        } catch (SecurityException e) {
            // 访问权限不足，无法检索到指定的系统属性值，打印警告信息
            logger.warn("Unable to retrieve a system property '{}'; default values will be used.", key, e);
        }

        // 如果为空，返回给定的默认值def
        if (value == null) {
            return def;
        }

        // 不为null，原样返回属性值
        return value;
    }

    /**
     * 返回boolean型的系统属性值
     *
     * Returns the value of the Java system property with the specified
     * {@code key}, while falling back to the specified default value if
     * the property access fails.
     *
     * @return the property value.
     *         {@code def} if there's no such property or if an access to the
     *         specified property is not allowed.
     */
    public static boolean getBoolean(String key, boolean def) {
        String value = get(key);
        if (value == null) {
            return def;
        }

        value = value.trim().toLowerCase();
        if (value.isEmpty()) {
            return def;
        }

        if ("true".equals(value) || "yes".equals(value) || "1".equals(value)) {
            return true;
        }

        if ("false".equals(value) || "no".equals(value) || "0".equals(value)) {
            return false;
        }

        logger.warn(
                "Unable to parse the boolean system property '{}':{} - using the default value: {}",
                key, value, def
        );

        return def;
    }

    /**
     * 返回指定{@code key}的Java系统属性整数型值，如果系统属性访问失败(未设置或不允许访问/无权限即返回值null)，则返回给定的缺省值{@code def}
     *
     * falling back 返回/退回/落回/回落
     *
     * Returns the value of the Java system property with the specified
     * {@code key}, while falling back to the specified default value if
     * the property access fails.
     *
     * @return the property value.
     *         {@code def} if there's no such property or if an access to the
     *         specified property is not allowed.
     */
    public static int getInt(String key, int def) {
        // 从JVM中检索key对相应的系统属性
        String value = get(key);

        // 如果为null，返回指定的默认值def
        if (value == null) {
            return def;
        }

        // 如果不为空，去空格，然后解析为整数
        value = value.trim();
        try {
            return Integer.parseInt(value);
        } catch (Exception e) {
            // Ignore
        }

        // 如果解析出现异常，即配置的系统属性不符合整数类型或其它问题，打印警告信息
        logger.warn(
                "Unable to parse the integer system property '{}':{} - using the default value: {}",
                key, value, def
        );

        // 打印警告信息后，返回默认值def
        return def;
    }

    /**
     * 返回long型的系统属性值
     *
     * Returns the value of the Java system property with the specified
     * {@code key}, while falling back to the specified default value if
     * the property access fails.
     *
     * @return the property value.
     *         {@code def} if there's no such property or if an access to the
     *         specified property is not allowed.
     */
    public static long getLong(String key, long def) {
        String value = get(key);
        if (value == null) {
            return def;
        }

        value = value.trim();
        try {
            return Long.parseLong(value);
        } catch (Exception e) {
            // Ignore
        }

        logger.warn(
                "Unable to parse the long integer system property '{}':{} - using the default value: {}",
                key, value, def
        );

        return def;
    }

    private SystemPropertyUtil() {
        // Unused
    }
}
