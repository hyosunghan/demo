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
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.EvaluationException;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.ParseException;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Aspect
@Component
public class RedisLockAspect {


    private static final ParameterNameDiscoverer nameDiscoverer = new DefaultParameterNameDiscoverer();

    private static final ExpressionParser parser = new SpelExpressionParser();

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private ThreadPoolTaskExecutor taskExecutor;

    @Pointcut(value = "@annotation(com.example.demo.lock.annotation.RedisLock)")
    public void pointCut() {}

    @Around(value = "pointCut() && @annotation(redisLock)")
    public Object around(ProceedingJoinPoint point, RedisLock redisLock) throws Throwable {
        String lockName = redisLock.name() + getSpElDefinitionValue(point, redisLock.keys());
        try {
            boolean getLock = taskExecutor.submit(() -> waitLock(redisLock, lockName)).get(redisLock.waitTime() + 1, TimeUnit.SECONDS);
            if (!getLock) {
                log.warn("锁[" + lockName + "]等待超时");
                return null;
            }
            return point.proceed();
        } catch (Exception e) {
            log.error("锁[" + lockName + "]执行异常: ", e);
            return null;
        } finally {
            redisTemplate.expire(lockName, 0, TimeUnit.SECONDS);
        }
    }

    private boolean waitLock(RedisLock lockInfo, String lockName) throws InterruptedException {
        long start = System.currentTimeMillis();
        do {
            Boolean flag = redisTemplate.opsForValue().setIfAbsent(lockName, "0", lockInfo.leaseTime(), TimeUnit.SECONDS);
            if (Boolean.TRUE.equals(flag)) {
                return true;
            }
            Thread.sleep(1000 / lockInfo.frequency());
        } while ((System.currentTimeMillis() - start) / 1000 < lockInfo.waitTime());
        return false;
    }

    public static String getSpElDefinitionValue(ProceedingJoinPoint joinPoint, String[] redisLockKeys) {
        try {
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
        } catch (Exception e) {
            log.error("解析锁参数异常，使用公共锁: ", e);
            return "";
        }
    }
}
