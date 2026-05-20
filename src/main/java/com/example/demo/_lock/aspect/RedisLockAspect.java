package com.example.demo._lock.aspect;

import com.example.demo._lock.annotation.RedisLock;
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
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Aspect
@Component
public class RedisLockAspect {

    private static final RedisScript<Long> RENEWAL_SCRIPT = new DefaultRedisScript<>(
            "if redis.call('get', KEYS[1]) == ARGV[1] then redis.call('expire', KEYS[1], tonumber(ARGV[2])) return 1 else return 0 end",
            Long.class
    );

    private static final RedisScript<Long> UNLOCK_SCRIPT = new DefaultRedisScript<>(
            "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end",
            Long.class
    );

    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1, r -> {
        Thread thread = new Thread(r);
        thread.setDaemon(true);
        thread.setName("redis-lock-renewal");
        return thread;
    });

    private static final ParameterNameDiscoverer discoverer = new DefaultParameterNameDiscoverer();

    private static final ExpressionParser parser = new SpelExpressionParser();

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Pointcut(value = "@annotation(com.example.demo._lock.annotation.RedisLock)")
    public void pointCut() {}

    @Around(value = "pointCut() && @annotation(redisLock)")
    public Object around(ProceedingJoinPoint point, RedisLock redisLock) throws Throwable {
        String lockName = redisLock.name() + getSpElDefinitionValue(point, redisLock.keys());
        String lockValue = UUID.randomUUID().toString();
        ScheduledFuture<?> scheduledFuture = null;
        int leaseTime = redisLock.leaseTime();
        boolean locked = waitingLock(lockName, lockValue, leaseTime, redisLock.waitTime(), redisLock.frequency());
        if (!locked) {
            throw new IllegalStateException("redis lock [" + lockName + "] getting failure.");
        }
        try {
            scheduledFuture = scheduler.scheduleAtFixedRate(() -> renewalLock(lockName, lockValue, leaseTime),
                    leaseTime * 750L, leaseTime * 750L, TimeUnit.MILLISECONDS);
            return point.proceed();
        } catch (Exception e) {
            throw e;
        } finally {
            if (scheduledFuture != null) {
                scheduledFuture.cancel(false);
            }
            unlock(lockName, lockValue);
        }
    }

    private boolean waitingLock(String lockName, String lockValue, int leaseTime, int waitTime, int frequency) throws InterruptedException {
        long start = System.currentTimeMillis();
        do {
            Boolean flag = redisTemplate.opsForValue().setIfAbsent(lockName, lockValue, leaseTime, TimeUnit.SECONDS);
            if (Boolean.TRUE.equals(flag)) {
                log.info("锁[{}]获取成功", lockName);
                return true;
            }
            Thread.sleep(1000 / frequency);
        } while ((System.currentTimeMillis() - start) / 1000 < waitTime);
        log.info("锁[{}]获取失败", lockName);
        return false;
    }

    private void renewalLock(String lockName, String lockValue, int leaseTime) {
        Long result = redisTemplate.execute(RENEWAL_SCRIPT, Collections.singletonList(lockName), lockValue, String.valueOf(leaseTime));
        if (result == 0) {
            log.error("锁[{}]续约失败", lockName);
            return;
        }
        log.info("锁[{}]续约成功", lockName);
    }

    private void unlock(String lockName, String lockValue) {
        Long result = redisTemplate.execute(UNLOCK_SCRIPT, Collections.singletonList(lockName), lockValue);
        if (result == 0) {
            log.error("锁[{}]释放失败", lockName);
            return;
        }
        log.info("锁[{}]释放成功", lockName);
    }

    public static String getSpElDefinitionValue(ProceedingJoinPoint joinPoint, String[] redisLockKeys) {
        try {
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            Method method = signature.getMethod();
            if (method.getDeclaringClass().isInterface()) {
                method = joinPoint.getTarget().getClass().getDeclaredMethod(signature.getName(), method.getParameterTypes());
            }
            Object[] parameterValues = joinPoint.getArgs();
            EvaluationContext context = new MethodBasedEvaluationContext(null, method, parameterValues, discoverer);
            List<String> serviceKeys = new ArrayList<>();
            for (String serviceKey : redisLockKeys) {
                if (serviceKey != null && !serviceKey.isEmpty()) {
                    Object value = parser.parseExpression(serviceKey).getValue(context);
                    serviceKeys.add(value == null ? "" : value.toString());
                }
            }
            return serviceKeys.stream().collect(Collectors.joining(":", ":", ""));
        } catch (Exception e) {
            log.error("解析锁参数异常，使用公共锁: ", e);
            return "";
        }
    }
}
