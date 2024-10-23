package com.example.demo.interceptor;

import org.apache.ibatis.executor.resultset.ResultSetHandler;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Signature;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Properties;

@Intercepts({
        @Signature(type = ResultSetHandler.class, method = "handleResultSets", args = {Statement.class})
})
@Component
public class ResultInterceptor implements Interceptor {

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        Object result = invocation.proceed();
        if (null == result) {
            return null;
        }
        if (result instanceof ArrayList) {
            ArrayList<?> resultList = (ArrayList<?>) result;
            for (Object o : resultList) {
                execute(o);
            }
        } else {
            execute(result);
        }
        return result;
    }

    @Override
    public Object plugin(Object o) {
        return null;
    }

    @Override
    public void setProperties(Properties properties) {

    }

    private void execute(Object result) throws IllegalAccessException {
        Class<?> resultClass = result.getClass();
        if (EncryptCommon.matchClass(resultClass.getName())) {
            Field[] declaredFields = resultClass.getDeclaredFields();
            for (Field field : declaredFields) {
                if (EncryptCommon.matchField(field.getName())) {
                    field.setAccessible(true);
                    Object object = field.get(result);
                    if (object instanceof String) {
                        String value = (String) object;
                        field.set(result, EncryptCommon.decrypt(value));
                    }
                }
            }
        }
    }
}
