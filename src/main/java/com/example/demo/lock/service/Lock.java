package com.example.demo.lock.service;

import com.example.demo.lock.model.LockInfo;
import org.aspectj.lang.ProceedingJoinPoint;

public interface Lock {

    Object proceed(ProceedingJoinPoint point, LockInfo lockInfo) throws Throwable ;

}
