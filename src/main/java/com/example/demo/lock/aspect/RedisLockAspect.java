package com.example.demo.lock.aspect;

import com.example.demo.lock.annotation.RedisLock;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.expression.MethodBasedEvaluationContext;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.data.redis.connection.RedisStringCommands;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.types.Expiration;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

@Slf4j
@Aspect
@Component
public class RedisLockAspect {


    private static final ParameterNameDiscoverer nameDiscoverer = new DefaultParameterNameDiscoverer();

    private static final ExpressionParser parser = new SpelExpressionParser();

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private ThreadPoolTaskExecutor taskExecutor;

    @Pointcut(value = "@annotation(com.example.demo.lock.annotation.RedisLock)")
    public void pointCut() {}

    @Around(value = "pointCut() && @annotation(redisLock)")
    public Object around(ProceedingJoinPoint point, RedisLock redisLock) throws Throwable {
        String lockName = redisLock.name() + getSpElDefinitionValue(point, redisLock.keys());
        Callable<Boolean> callable = () -> waitLock(redisLock, lockName);
        try {
            Future<Boolean> future = taskExecutor.submit(callable);
            future.get(redisLock.waitTime(), TimeUnit.SECONDS);
            try {
                return point.proceed();
            } catch (Exception e) {
                throw e;
            } finally {
                redisExpire(lockName);
            }
        } catch (TimeoutException e) {
            throw new RuntimeException("获取锁超时");
        }
    }

    private Boolean waitLock(RedisLock lockInfo, String lockName) {
        int i = 0;
        do {
            try {
                i++;
                log.debug("第{}次尝试获取{}事务锁！！", i, lockName);
                boolean flag = redisSetNX(lockName, lockInfo.value(), lockInfo.leaseTime());
                if (flag) {
                    return true;
                }
                Thread.sleep(1000 / lockInfo.frequency());
            } catch (Exception e) {
                log.error("获取锁出现异常：", e);
            }
        } while (i < lockInfo.waitTime() * lockInfo.frequency());
        return false;
    }

    public Boolean redisSetNX(String k, String v, Integer timeout) {
        return stringRedisTemplate.execute((RedisCallback<Boolean>) conn -> conn.set(k.getBytes(), v.getBytes(),
                Expiration.from(timeout, TimeUnit.SECONDS), RedisStringCommands.SetOption.SET_IF_ABSENT));
    }

    private Boolean redisExpire(String k) {
        return stringRedisTemplate.expire(k, 0, TimeUnit.SECONDS);
    }

    public static String getSpElDefinitionValue(ProceedingJoinPoint joinPoint, String[] redisLockKeys) throws NoSuchMethodException {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        if (method.getDeclaringClass().isInterface()) {
            method = joinPoint.getTarget().getClass().getDeclaredMethod(signature.getName(), method.getParameterTypes());
        }
        Object[] parameterValues = joinPoint.getArgs();
        EvaluationContext context = new MethodBasedEvaluationContext(null, method, parameterValues, nameDiscoverer);
        List<String> serviceKeys = new ArrayList<>();
        for (String serviceKey : redisLockKeys) {
            if (serviceKey != null && !serviceKey.isEmpty()) {
                Object value = parser.parseExpression(serviceKey).getValue(context);
                serviceKeys.add(value == null ? "" : value.toString());
            }
        }
        return serviceKeys.stream().collect(Collectors.joining(":", ":", ""));
    }
}
