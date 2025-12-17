package com.togettoyou.fabricrealty.springbootserver.block.listener;

import org.hyperledger.fabric.client.Network;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Iterator;

public final class BlockEventsAccessor {
    private BlockEventsAccessor() {
    }

    public static BlockEventStream open(Network network, long startBlock) {
        Object events = createEventsSource(network, startBlock);
        Iterator<?> iterator = toIterator(events);
        AutoCloseable closeable = toCloseable(events);
        return new BlockEventStream(iterator, closeable);
    }

    private static Object createEventsSource(Network network, long startBlock) {
        try {
            Method direct = findMethod(network.getClass(), "getBlockEvents", long.class);
            if (direct != null) {
                return direct.invoke(network, startBlock);
            }
            direct = findMethod(network.getClass(), "getBlockEvents", Long.class);
            if (direct != null) {
                return direct.invoke(network, startBlock);
            }

            direct = findMethod(network.getClass(), "blockEvents", long.class);
            if (direct != null) {
                return direct.invoke(network, startBlock);
            }
            direct = findMethod(network.getClass(), "blockEvents", Long.class);
            if (direct != null) {
                return direct.invoke(network, startBlock);
            }

            Method newRequest = findMethod(network.getClass(), "newBlockEventsRequest");
            if (newRequest != null) {
                Object builder = newRequest.invoke(network);
                Object configured = tryInvoke(builder, "startBlock", long.class, startBlock);
                if (configured == null) {
                    configured = tryInvoke(builder, "startBlock", Long.class, startBlock);
                }
                if (configured == null) {
                    configured = builder;
                }

                Object request = tryInvokeNoArgs(configured, "build");
                if (request == null) {
                    request = configured;
                }
                Object events = tryInvokeNoArgs(request, "getEvents");
                if (events != null) {
                    return events;
                }
                events = tryInvokeNoArgs(configured, "getEvents");
                if (events != null) {
                    return events;
                }
                return request;
            }

            throw new UnsupportedOperationException("当前 fabric-gateway Java SDK 未找到 block events API（getBlockEvents/newBlockEventsRequest）");
        } catch (InvocationTargetException e) {
            Throwable target = e.getTargetException();
            if (target instanceof RuntimeException re) {
                throw re;
            }
            throw new RuntimeException(target);
        } catch (Exception e) {
            if (e instanceof RuntimeException re) {
                throw re;
            }
            throw new RuntimeException(e);
        }
    }

    private static Iterator<?> toIterator(Object events) {
        if (events == null) {
            throw new IllegalStateException("block events 结果为空");
        }
        if (events instanceof Iterator<?> iterator) {
            return iterator;
        }
        if (events instanceof Iterable<?> iterable) {
            return iterable.iterator();
        }
        Method iteratorMethod = findMethod(events.getClass(), "iterator");
        if (iteratorMethod != null) {
            try {
                Object iterator = iteratorMethod.invoke(events);
                if (iterator instanceof Iterator<?> it) {
                    return it;
                }
            } catch (Exception ignored) {
            }
        }
        Method hasNext = findMethod(events.getClass(), "hasNext");
        Method next = findMethod(events.getClass(), "next");
        if (hasNext != null && next != null) {
            return new ReflectionIterator(events, hasNext, next);
        }
        throw new IllegalStateException("block events 返回对象不可迭代: " + events.getClass().getName());
    }

    private static AutoCloseable toCloseable(Object events) {
        if (events instanceof AutoCloseable closeable) {
            return closeable;
        }
        Method close = findMethod(events.getClass(), "close");
        if (close != null) {
            return () -> {
                try {
                    close.invoke(events);
                } catch (Exception ignored) {
                }
            };
        }
        return null;
    }

    private static Method findMethod(Class<?> type, String name, Class<?>... parameterTypes) {
        try {
            return type.getMethod(name, parameterTypes);
        } catch (NoSuchMethodException ignored) {
            return null;
        }
    }

    private static Object tryInvoke(Object target, String name, Class<?> parameterType, Object argument) {
        Method method = findMethod(target.getClass(), name, parameterType);
        if (method == null) {
            return null;
        }
        try {
            return method.invoke(target, argument);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static Object tryInvokeNoArgs(Object target, String name) {
        Method method = findMethod(target.getClass(), name);
        if (method == null) {
            return null;
        }
        try {
            return method.invoke(target);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static final class ReflectionIterator implements Iterator<Object> {
        private final Object target;
        private final Method hasNext;
        private final Method next;

        private ReflectionIterator(Object target, Method hasNext, Method next) {
            this.target = target;
            this.hasNext = hasNext;
            this.next = next;
        }

        @Override
        public boolean hasNext() {
            try {
                Object result = hasNext.invoke(target);
                return result instanceof Boolean b && b;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public Object next() {
            try {
                return next.invoke(target);
            } catch (InvocationTargetException e) {
                Throwable target = e.getTargetException();
                if (target instanceof RuntimeException re) {
                    throw re;
                }
                throw new RuntimeException(target);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }
}
