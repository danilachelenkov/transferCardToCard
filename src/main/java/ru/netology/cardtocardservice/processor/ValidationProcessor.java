package ru.netology.cardtocardservice.processor;

import ru.netology.cardtocardservice.annotation.DateValidType;
import ru.netology.cardtocardservice.annotation.ValidityDatePeriod;
import ru.netology.cardtocardservice.domain.TransferInfo;
import ru.netology.cardtocardservice.exception.DateInvalidException;
import ru.netology.cardtocardservice.exception.UnknownValidTypeException;

import java.lang.reflect.Field;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class ValidationProcessor {
    public static void validateTransferInfo(TransferInfo transferInfo) throws IllegalAccessException, ParseException {
        Field[] fields = transferInfo.getClass().getDeclaredFields();
        for (Field field : fields) {
            if (field.isAnnotationPresent(ValidityDatePeriod.class)) {
                ValidityDatePeriod annotation = field.getAnnotation(ValidityDatePeriod.class);

                if (annotation.typeValid().equals(DateValidType.MMYY)) {
                    String message = annotation.message();
                    validatePeriodActionCard(transferInfo, field, message);
                } else {
                    throw new UnknownValidTypeException("Set the unknown type DateValidType", 503);
                }

            }
        }
    }

    public static void validatePeriodActionCard(TransferInfo transferInfo, Field field, String message) throws IllegalAccessException, ParseException {

        field.setAccessible(true);

        String name = (String) field.get(transferInfo);
        String dateCard = "01." + name.substring(0, 2) + ".20" + name.substring(2);
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy");

        Date convertedDate = dateFormat.parse(dateCard);

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(convertedDate);
        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH));

        if (new Date().after(calendar.getTime())) {
            throw new DateInvalidException(message, 110);
        }

        field.setAccessible(false);
    }
}
