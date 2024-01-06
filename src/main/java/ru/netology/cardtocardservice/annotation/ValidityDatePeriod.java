package ru.netology.cardtocardservice.annotation;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
@Documented
public @interface ValidityDatePeriod {
    String message();

    DateValidType typeValid();
}





