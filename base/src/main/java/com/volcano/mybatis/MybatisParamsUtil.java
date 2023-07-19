package com.volcano.mybatis;

import java.text.DateFormat;
import java.util.Date;
import java.util.Locale;

public class MybatisParamsUtil {

    /**
     * 如果参数是String，则添加单引号， 如果是日期，则转换为时间格式器并加单引号；
     * 对参数是null和不是null的情况作了处理<br>
     *
     * @param obj
     * @return
     */
    public static String getParameterValue(Object obj) {
        String value = null;
        if (obj instanceof String) {
            value = "'" + obj.toString() + "'";
        } else if (obj instanceof Date) {
            DateFormat formatter = DateFormat.getDateTimeInstance(DateFormat.DEFAULT, DateFormat.DEFAULT, Locale.CHINA);
            value = "'" + formatter.format(new Date()) + "'";
        }/*else if(obj instanceof Collection || obj.class.isArray()){
            value = "'"+String.join("','")+"'";
        }*/ else {
            if (obj != null) {
                value = obj.toString();
            } else {
                value = "null";
            }

        }
        return value;
    }
}
