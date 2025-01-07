package com.example.demo.lock.aspect;

import com.example.demo.lock.model.LockInfo;
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

    private static final String LOCK_VALUE = "0";

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private ThreadPoolTaskExecutor taskExecutor;

    @Pointcut(value = "@annotation(com.example.demo.lock.annotation.RedisLock)")
    public void pointCut() {}

    @Around(value = "pointCut() && @annotation(redisLock)")
    public Object around(ProceedingJoinPoint point, RedisLock redisLock) throws Throwable {
        LockInfo lockInfo = new LockInfo();
        lockInfo.setFrequency(redisLock.frequency());
        lockInfo.setLeaseTime(redisLock.leaseTime());
        lockInfo.setWaitTime(redisLock.waitTime());
        lockInfo.setLockValue(LOCK_VALUE);
        lockInfo.setLockName(redisLock.name() + getSpElDefinitionValue(point, redisLock.keys()));
        switch (redisLock.type()) {
            case EXEC:
                return exec(point, lockInfo);
            case WAIT:
            default:
                return wait(point, lockInfo);
        }
    }

    private Object wait(ProceedingJoinPoint point, LockInfo lockInfo) throws Throwable  {
        String lockKey = lockInfo.getLockName();
        Callable<Boolean> callable = () -> waitResult(lockInfo);
        try {
            Future<Boolean> future = taskExecutor.submit(callable);
            future.get(lockInfo.getWaitTime(), TimeUnit.SECONDS);
            try {
                return point.proceed();
            } catch (Exception e) {
                throw e;
            } finally {
                redisExpire(lockKey);
            }
        } catch (TimeoutException e) {
            throw new RuntimeException("获取锁超时");
        }
    }

    private Boolean waitResult(LockInfo lockInfo) {
        int i = 0;
        int sleep = 1000 / lockInfo.getFrequency();
        do {
            try {
                i++;
                log.debug("第{}次尝试获取{}事务锁！！", i, lockInfo.getLockName());
                boolean flag = redisSetNX(lockInfo.getLockName(), lockInfo.getLockValue(), lockInfo.getLeaseTime());
                if (flag) {
                    return true;
                }
                Thread.sleep(sleep);
            } catch (Exception e) {
                log.error("获取锁出现异常：", e);
            }
        } while (i < (lockInfo.getWaitTime() + 1) * lockInfo.getFrequency());
        return false;
    }

    private Object exec(ProceedingJoinPoint point, LockInfo lockInfo) throws Throwable  {
        boolean flag = redisSetNX(lockInfo.getLockName(), lockInfo.getLockValue(), lockInfo.getLeaseTime());
        log.debug("尝试获取 {} 事务锁的结果为：{}", lockInfo.getLockName(), flag);
        if (!flag) {
            Long l = redisIncrement(lockInfo.getLockName());
            log.debug("未获取 {} 事务锁，自增结果为：{}", lockInfo.getLockName(), l);
            throw new RuntimeException("获取锁失败");
        }
        try {
            return point.proceed();
        } finally {
            String v = redisGet(lockInfo.getLockName());
            log.debug("{} 事务锁处理完成，处理过程中被拦截 {} 次！", lockInfo.getLockName(), v);
            redisExpire(lockInfo.getLockName());
        }
    }

    public Boolean redisSetNX(String k, String v, Integer timeout) {
        return stringRedisTemplate.execute((RedisCallback<Boolean>) conn -> conn.set(k.getBytes(), v.getBytes(),
                Expiration.from(timeout, TimeUnit.SECONDS), RedisStringCommands.SetOption.SET_IF_ABSENT));
    }

    private Boolean redisExpire(String k) {
        return stringRedisTemplate.expire(k, 0, TimeUnit.SECONDS);
    }

    private Long redisIncrement(String k) {
        return stringRedisTemplate.opsForValue().increment(k, 1L);
    }

    public String redisGet(String k) {
        return stringRedisTemplate.opsForValue().get(k);
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
                String string = parser.parseExpression(serviceKey).getValue(context).toString();
                serviceKeys.add(string);
            }
        }
        return serviceKeys.stream().collect(Collectors.joining(":", ":", ""));
    }
}
