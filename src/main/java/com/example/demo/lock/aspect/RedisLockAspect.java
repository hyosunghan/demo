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

    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    private static final ParameterNameDiscoverer discoverer = new DefaultParameterNameDiscoverer();

    private static final ExpressionParser parser = new SpelExpressionParser();

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Pointcut(value = "@annotation(com.example.demo.lock.annotation.RedisLock)")
    public void pointCut() {}

    @Around(value = "pointCut() && @annotation(redisLock)")
    public Object around(ProceedingJoinPoint point, RedisLock redisLock) throws Throwable {
        String lockName = redisLock.name() + getSpElDefinitionValue(point, redisLock.keys());
        String lockValue = UUID.randomUUID().toString();
        boolean locked = false;
        ScheduledFuture<?> scheduledFuture = null;
        try {
            locked = waitingLock(redisLock, lockName, lockValue);
            if (!locked) {
                log.warn("锁[" + lockName + "]等待超时");
                return null;
            }
            int leaseTime = redisLock.leaseTime();
            scheduledFuture = scheduler.scheduleAtFixedRate(() -> renewalLock(lockName, lockValue, leaseTime),
                    leaseTime / 2, leaseTime / 2, TimeUnit.SECONDS);
            return point.proceed();
        } catch (Exception e) {
            log.error("锁[" + lockName + "]执行异常: ", e);
            return null;
        } finally {
            if (locked) {
                unlock(lockName, lockValue);
            }
            if (scheduledFuture != null) {
                scheduledFuture.cancel(true);
            }
        }
    }

    private boolean waitingLock(RedisLock lockInfo, String lockName, String lockValue) throws InterruptedException {
        long start = System.currentTimeMillis();
        do {
            Boolean flag = redisTemplate.opsForValue().setIfAbsent(lockName, lockValue, lockInfo.leaseTime(), TimeUnit.SECONDS);
            if (Boolean.TRUE.equals(flag)) {
                return true;
            }
            Thread.sleep(1000 / lockInfo.frequency());
        } while ((System.currentTimeMillis() - start) / 1000 < lockInfo.waitTime());
        return false;
    }

    private void renewalLock(String lockName, String lockValue, int leaseTime) {
        try {
            Long result = redisTemplate.execute(RENEWAL_SCRIPT, Collections.singletonList(lockName), lockValue, String.valueOf(leaseTime));
            if (result == null || result == 0) {
                return;
            }
            log.info("自动续期锁[" + lockName + "]成功");
        } catch (Exception e) {
            log.error("自动续期锁[" + lockName + "]失败: ", e);
        }
    }

    private void unlock(String lockName, String lockValue) {
        redisTemplate.execute(UNLOCK_SCRIPT, Collections.singletonList(lockName), lockValue);
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
