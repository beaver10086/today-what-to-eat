package com.studyroom.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.studyroom.common.ApiResponse;
import com.studyroom.common.AuditAction;
import com.studyroom.common.AuthUser;
import com.studyroom.mapper.CanteenMapper;
import com.studyroom.mapper.DishMapper;
import com.studyroom.mapper.OperationLogMapper;
import com.studyroom.mapper.ShopMapper;
import com.studyroom.mapper.TagMapper;
import com.studyroom.model.OperationLog;
import jakarta.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;
import java.util.Map;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Aspect
@Component
public class AuditActionAspect {
    private final OperationLogMapper operationLogMapper;
    private final CanteenMapper canteenMapper;
    private final ShopMapper shopMapper;
    private final DishMapper dishMapper;
    private final TagMapper tagMapper;
    private final ObjectMapper objectMapper;

    public AuditActionAspect(OperationLogMapper operationLogMapper, CanteenMapper canteenMapper,
                             ShopMapper shopMapper, DishMapper dishMapper, TagMapper tagMapper,
                             ObjectMapper objectMapper) {
        this.operationLogMapper = operationLogMapper;
        this.canteenMapper = canteenMapper;
        this.shopMapper = shopMapper;
        this.dishMapper = dishMapper;
        this.tagMapper = tagMapper;
        this.objectMapper = objectMapper;
    }

    @Around("@annotation(auditAction)")
    public Object recordOperation(ProceedingJoinPoint point, AuditAction auditAction) throws Throwable {
        Long targetId = firstId(point.getArgs());
        Object before = targetId == null ? null : loadBefore(auditAction.module(), targetId);
        Object result = point.proceed();
        if (targetId == null) {
            targetId = returnedId(result);
        }
        if (targetId != null) {
            saveLog(auditAction, targetId, before, result);
        }
        return result;
    }

    private Object loadBefore(String module, Long id) {
        return switch (module) {
            case "canteen" -> canteenMapper.selectById(id);
            case "shop" -> shopMapper.selectById(id);
            case "dish" -> dishMapper.selectById(id);
            case "tag" -> tagMapper.selectById(id);
            default -> null;
        };
    }

    private void saveLog(AuditAction action, Long targetId, Object before, Object result)
            throws JsonProcessingException {
        OperationLog log = new OperationLog();
        log.setOperatorId(operatorId());
        log.setModule(action.module());
        log.setAction(action.action());
        log.setTargetId(targetId);
        log.setBeforeData(before == null ? null : objectMapper.writeValueAsString(before));
        Object after = action.action() == 3 ? Map.of("id", targetId, "isDeleted", 1) : unwrap(result);
        log.setAfterData(after == null ? null : objectMapper.writeValueAsString(after));
        log.setIp(remoteAddress());
        operationLogMapper.insert(log);
    }

    private static Long firstId(Object[] args) {
        for (Object arg : args) {
            if (arg instanceof Long id) {
                return id;
            }
        }
        return null;
    }

    private static Long returnedId(Object result) {
        Object payload = unwrap(result);
        if (payload == null) {
            return null;
        }
        try {
            Method accessor = payload.getClass().getMethod("id");
            Object id = accessor.invoke(payload);
            return id instanceof Long value ? value : null;
        } catch (ReflectiveOperationException exception) {
            return null;
        }
    }

    private static Object unwrap(Object result) {
        if (result instanceof ApiResponse<?> response) {
            return response.data();
        }
        return result;
    }

    private static long operatorId() {
        HttpServletRequest request = request();
        Object principal = request == null ? null : request.getAttribute(AuthUser.REQUEST_ATTRIBUTE);
        return principal instanceof AuthUser user ? user.id() : 0L;
    }

    private static String remoteAddress() {
        HttpServletRequest request = request();
        return request == null ? null : request.getRemoteAddr();
    }

    private static HttpServletRequest request() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
            return attributes.getRequest();
        }
        return null;
    }
}
