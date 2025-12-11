package com.upec.factoryscheduling.common.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.SQLException;
import java.text.Collator;
import java.text.Format;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;


/**
 * @author zhangk
 * 系统工作处理函数
 */
public class PubFun {


    public static List removeDullicateWithOther(List list) {
        HashSet set = new HashSet();
        List nlist = new ArrayList();
        for (Iterator i = list.iterator(); i.hasNext(); ) {
            Object o = i.next();
            if (set.add(o)) {
                nlist.add(o);
            }
        }
        return nlist;
    }

    /**
     * 判断两时间是否在同一区间内
     *
     * @param d1
     * @param d2
     * @param step 单位秒
     *             aaaa
     */
    public static boolean arear2Date(Date d1, Date d2, double step) {
        long m = (long) (step * 1000 + 1);
        long n = Math.abs(d1.getTime() - d2.getTime());
        return (n < m);
    }

    /**
     * 若量时间处于同一区间（STEP）内，则返回较晚的时间。
     *
     * @param d1   日期1
     * @param d2   日期2
     * @param step 单位秒
     */
    public static Date bigIn2DateByStep(Date d1, Date d2, double step) {
        long m = (long) (step * 1000 + 1);
        long n = Math.abs(d1.getTime() - d2.getTime());
        if (n < m) {
            return d1.before(d2) ? d2 : d1;
        }
        return null;
    }

    /**
     * 计算某个日期的前后几天(包含当天)
     *
     * @param date
     * @param day
     * @return
     */
    public static List<String> computeDateList(Date date, int day) {
        List<String> list = new ArrayList();
        int dayABS = Math.abs(day);
        int countValue = 0;
        if (day < 0) {
            countValue = day + 1;
        }
        int countValueStar = countValue;
        while (dayABS != countValue - countValueStar) {
            String querydate = computeDate(date, countValue);
            list.add(querydate);
            countValue++;
        }
        return list;
    }

    /**
     * 计算某个日期的几天前或者几天后
     *
     * @param date
     * @param day
     * @return
     */
    public static String computeDate(Date date, int day) {
        Format format = new SimpleDateFormat("yyyy-MM-dd");
        Calendar c = Calendar.getInstance();
        c.setTime(date);
        c.add(c.DATE, day);
        return format.format(c.getTime());
    }

    /**
     * 计算某周的周一和周日
     *
     * @param date
     * @return map:firstday,lastday
     */
    public static Map<String, String> computeFirstAndLastDayofWeek(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.setFirstDayOfWeek(Calendar.MONDAY);
        int dayweek = cal.get(Calendar.DAY_OF_WEEK);
        if (1 == dayweek) {
            cal.add(Calendar.DAY_OF_MONTH, -1);
        }
        cal.setFirstDayOfWeek(Calendar.MONDAY);
        int day = cal.get(Calendar.DAY_OF_WEEK);
        cal.add(Calendar.DATE, cal.getFirstDayOfWeek() - day);
        String firstday = Date2String(cal.getTime(), "yyyy-MM-dd");
        cal.add(Calendar.DATE, 6);
        String lastday = Date2String(cal.getTime(), "yyyy-MM-dd");
        Map m = new HashMap();
        m.put("firstday", firstday);
        m.put("lastday", lastday);
        return m;
    }


    public static String encodeURI(String url) {
        try {
            return new String(url.getBytes("iso-8859-1"), "utf-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static Map MergeMap(Map<String, Object> sourceMap, Map<String, Object> targetMap) {
        Set<String> set = sourceMap.keySet();
        for (String str : set) {
            targetMap.put(str, sourceMap.get(str));
        }
        return targetMap;

    }

    public static Object getValueFromArray(Object object) {
        return ((Object[]) object)[0];
    }

    @SuppressWarnings("unchecked")
    public static Map MapArray2MapObject(Map imap) {
        Map result = new HashMap();
        Set<String> set = imap.keySet();
        for (String str : set) {
            result.put(str, ((Object[]) imap.get(str))[0]);
        }
        return result;
    }

    /**
     * 流水号自增长
     *
     * @param number
     * @return
     */
    public static String gainSerialNumber(String number) {
        int length = number.length();
        int numberInt = Integer.parseInt(number);
        numberInt = numberInt + 1;
        String numberStr = String.valueOf(numberInt);
        for (int i = numberStr.length(); i < length; i++) {
            numberStr = "0" + numberStr;
        }
        return numberStr;
    }


    /**
     * 使用MDS加密
     */
    public static String encrytion16(String plaintext) {
        StringBuilder sb = new StringBuilder("256");
        try {
            MessageDigest instance = MessageDigest.getInstance("MD5");
            instance.update(plaintext.getBytes());

            byte b[] = instance.digest();
            int tmp;

            for (byte value : b) {
                tmp = value;
                if (tmp < 0) {
                    tmp += 256;
                }
                if (tmp < 16) {
                    sb.append("0");
                }
                sb.append(Integer.toHexString(tmp));
            }

        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return sb.toString().substring(8, 24);
    }


    @SuppressWarnings("unchecked")
    public static Map getMapFromBlob(Blob blob) {
        Map result = null;
        ObjectInputStream ois = null;
        try {
            ois = new ObjectInputStream(blob.getBinaryStream());
            result = (Map<?, ?>) ois.readObject();

        } catch (IOException | ClassNotFoundException | SQLException e) {
            e.printStackTrace();
        } finally {
            try {
                assert ois != null;
                ois.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return result;
    }


    /**
     * 得到当前系统日期
     *
     * @return 当前日期的格式字符串, 日期格式为"yyyy-MM-dd"
     */
    public static String getCurrentDate(String pattern) {
        SimpleDateFormat df = new SimpleDateFormat(pattern);
        Date today = new Date();
        return df.format(today);
    }

    /**
     * 得到当前系统时间
     *
     * @return 当前时间的格式字符串，时间格式为"HH:mm:ss"
     */
    public static String getCurrentTime() {
        String pattern = "HH:mm:ss";
        SimpleDateFormat df = new SimpleDateFormat(pattern);
        Date today = new Date();
        return df.format(today);
    }

    /**
     * date 转 string
     *
     * @param format 'yyyy-MM-dd HH:mm:ss'|'yyyy-MM-dd'
     */
    public static String Date2String(Date d, String format) {
        SimpleDateFormat df = new SimpleDateFormat(format);
        return df.format(d);
    }

    /**
     * string 转 date
     *
     * @param format 'yyyy-MM-dd HH:mm:ss'|'yyyy-MM-dd'
     */
    public static Date String2Date(String date, String format) {
        SimpleDateFormat df = new SimpleDateFormat(format);
        if (date.equals("")) return null; //遇到表单时间空值是会抛异常
        try {
            return df.parse(date);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 得到当前系统时间
     *
     * @return 当前时间的格式字符串，时间格式为"yyyy-MM-dd HH:mm:ss"
     */
    public static String getCurrent() {
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date today = new Date();
        return df.format(today);
    }


    public static String unescape(String str) {


        StringBuffer sb = new StringBuffer();
        sb.ensureCapacity(str.length());
        int lastPos = 0, pos = 0, nLen = str.length();
        char ch;
        while (lastPos < nLen) {
            pos = str.indexOf("%", lastPos);
            if (pos == lastPos) {
                if (str.charAt(pos + 1) == 'u') {
                    ch = (char) Integer.parseInt(str.substring(pos + 2, pos + 6), 16);
                    sb.append(ch);
                    lastPos = pos + 6;
                } else {
                    ch = (char) Integer.parseInt(str.substring(pos + 1, pos + 3), 16);
                    sb.append(ch);
                    lastPos = pos + 3;
                }
            } else {
                if (pos == -1) {
                    sb.append(str.substring(lastPos));
                    lastPos = nLen;
                } else {
                    sb.append(str.substring(lastPos, pos));
                    lastPos = pos;
                }
            }
        }

        return sb.toString();
    }

    /**
     * map排序
     *
     * @param data
     * @param indexattr
     */
    @SuppressWarnings("unchecked")
    public static List<Map> sortlistmap(List<Map> data, String indexattr) {

        final String attr = indexattr;
        Comparator<Map> p = new Comparator<Map>() {
            public int compare(Map o1, Map o2) {
                Comparator p = Collator.getInstance(Locale.CHINA);
                return p.compare(o1.get(attr).toString(), o2.get(attr).toString());
            }
        };
        Collections.sort(data, p);
        return data;
    }


    /**
     * 大写转小写
     *
     * @return
     */
    @SuppressWarnings({"unchecked"})
    public List<Map> queryKeyUppercase2Lowercase(List<Map> queryMap) {
        List<Map> resultMap = new ArrayList();
        for (Map imap : queryMap) {
            Map param = new HashMap();
            for (Object obj : imap.keySet()) {
                param.put(obj.toString().toLowerCase(), imap.get(obj));
            }
            resultMap.add(param);
        }
        return resultMap;
    }

    /**
     * 将clob反序列化
     *
     * @param content
     * @return
     */
    public static String clob2String(Clob content) {
        if (content == null) {
            return null;
        }
        StringBuffer contentStr = new StringBuffer(256);
        BufferedReader reader = null;
        char[] tempchars = new char[32];
        int charread = 0;

        try {
            reader = new BufferedReader(content.getCharacterStream());

            // 读入多个字符到字符数组中，charread为一次读取字符数
            while ((charread = reader.read(tempchars)) != -1) {
                for (int j = 0; j < charread; j++) {
                    contentStr.append(tempchars[j]);
                }
            }
        } catch (SQLException e1) {
            e1.printStackTrace();
        } catch (IOException e2) {
            e2.printStackTrace();
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e1) {
                }
            }

        }
        return contentStr.toString();
    }

    /**
     * pojo转map
     *
     * @param obj
     * @return
     */
    public static Map pojo2map(Object obj) {
        Map rsmap = new HashMap();
        Class clz = obj.getClass();
        Method[] methods = clz.getDeclaredMethods();
        for (Method method : methods) {
            String methodname = method.getName();
            if (methodname.startsWith("get")) {
                String mapkey = methodname.substring(3);
                try {
                    rsmap.put(mapkey.toLowerCase(), method.invoke(obj, new Object[0]));
                } catch (Exception e) {

                }

            } else {
                continue;
            }
        }

        return rsmap;
    }

    /**
     * 提供相对精度的除法运算，可四舍五入
     *
     * @param v1    被除数
     * @param v2    除数
     * @param scale 需要精确到小数点后几位,必须不小于0
     * @return 两个参数的商
     */
    public static double div(double v1, double v2, int scale) {
        if (v2 == Double.parseDouble("0")) {
            return 0;
        }
        BigDecimal b1 = new BigDecimal(Double.toString(v1));
        BigDecimal b2 = new BigDecimal(Double.toString(v2));
        return b1.divide(b2, scale, BigDecimal.ROUND_HALF_UP).doubleValue();
    }


    /**
     * 计算百分比
     *
     * @param firstnum  被除数
     * @param secondnum 除数
     * @param scale     需要精确到小数点后几位,必须不小于0
     * @return
     */
    public static String getPercent(double firstnum, double secondnum, int scale) {
        double percenNum = div(firstnum, secondnum, scale + 3);
        String percentStr = String.valueOf(percenNum * 100);
        if (percentStr.length() > (scale + 3)) {
            String a = "";
            String aa = percentStr.substring(0, percentStr.indexOf(".")); //整数部分
            if (aa.length() >= 3) { //结果大于1
                String pointStr = percentStr.substring(percentStr.indexOf(".") + 1, percentStr.length());
                if (pointStr.length() > scale) {//小数部分长度大于精度要求
                    String bb = pointStr.substring(0, scale);
                    a = aa + "." + bb;
                } else {
                    a = percentStr;
                }
            } else {
                a = percentStr.substring(0, percentStr.indexOf(".") + scale + 3 - 1);
            }
            double percentNumNew = div(Double.valueOf(a), Double.valueOf("1"), scale);
            percentStr = String.valueOf(percentNumNew) + "%";
        } else {
            percentStr = percentStr + "%";
        }
        return percentStr;
    }

    /**
     * 时间比较，两个date相差几天
     *
     * @param startDate 开始时间
     * @param endDate   结束时间
     * @return 天数
     */
    public static int daySubDay(Date startDate, Date endDate) {
        int diff_time = 0;
        if (startDate != null && endDate != null) {
            long start_long = startDate.getTime();
            long end_long = endDate.getTime();
            diff_time = new Long((end_long - start_long) / (24 * 1000 * 60 * 60)).intValue();
        }
        return diff_time;
    }

    /**
     * 日期比较，两个date相差几天
     *
     * @return 天数
     * @throws ParseException
     */
    public static int daySubDay(String startDate, String endDate) throws ParseException {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        Calendar cal = Calendar.getInstance();
        cal.setTime(sdf.parse(startDate));
        long time1 = cal.getTimeInMillis();
        cal.setTime(sdf.parse(endDate));
        long time2 = cal.getTimeInMillis();
        long days = (time2 - time1) / (1000 * 3600 * 24);
        return Integer.parseInt(String.valueOf(days));
    }


    /**
     * 数组取最小值
     */
    public static String findMinInteger(int[] intArray) {
        if (intArray.length > 0) {
            int min = intArray[0];
            for (int i = 1; i < intArray.length; i++) {
                if (intArray[i] < min) {
                    min = intArray[i];
                }
            }
            return String.valueOf(min);
        }
        return null;

    }

}
