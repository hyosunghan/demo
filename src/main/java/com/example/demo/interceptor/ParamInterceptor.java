package com.example.demo.interceptor;

import org.apache.ibatis.executor.parameter.ParameterHandler;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Signature;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.sql.PreparedStatement;
import java.util.Properties;

@Intercepts({
        @Signature(type = ParameterHandler.class, method = "setParameters", args = {PreparedStatement.class})
})
@Component
public class ParamInterceptor implements Interceptor {
    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        if (invocation.getTarget() instanceof ParameterHandler) {
            ParameterHandler parameterHandler = (ParameterHandler) invocation.getTarget();
            Field parameterField = parameterHandler.getClass().getDeclaredField("parameterObject");
            parameterField.setAccessible(true);
            Object parameterObject = parameterField.get(parameterHandler);
            if (parameterObject != null) {
                Class<?> parameterObjectClass = parameterObject.getClass();
                if (EncryptCommon.matchClass(parameterObjectClass.getName())) {
                    Field[] declaredFields = parameterObjectClass.getDeclaredFields();
                    for (Field field : declaredFields) {
                        if (EncryptCommon.matchField(field.getName())) {
                            field.setAccessible(true);
                            Object object = field.get(parameterObject);
                            if (object instanceof String) {
                                String value = (String) object;
                                field.set(parameterObject, EncryptCommon.encrypt(value));
                            }
                        }
                    }
                }
            }
        }
        return invocation.proceed();
    }

    @Override
    public Object plugin(Object o) {
        return null;
    }

    @Override
    public void setProperties(Properties properties) {

    }


}
