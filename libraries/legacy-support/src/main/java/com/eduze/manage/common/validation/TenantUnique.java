package com.eduze.manage.common.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Constraint(validatedBy = TenantUniqueValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface TenantUnique {

    String message() default "值已存在";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    String table();

    String column();

    /** Bean property name holding entity id for update (e.g. excludeId). Empty = create only. */
    String excludeIdProperty() default "";
}
