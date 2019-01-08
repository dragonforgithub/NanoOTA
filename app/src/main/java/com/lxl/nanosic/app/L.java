package com.lxl.nanosic.app;


import android.text.TextUtils;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.StringReader;
import java.io.StringWriter;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

/**
 * 日志管理类
 */
public final class L {
    private static final int JSON_INDENT = 4;
    private static boolean isEnable = true;         // 显示或隐藏日志
    //private static String tag = "RemoteUpgrade";    // 默认Log
    private static String tag = "NanoOTA";    // 默认Log

    /**
     * 设置tag
     * <p>
     * 推荐在Application中做初始化
     */
    public static void setTag(String tag) {
        L.tag = tag;
    }

    /**
     * 设置Log是否显示
     *
     * @param loggable 　boolean
     */
    public static void setLoggable(boolean loggable) {
        L.isEnable = loggable;
    }

    /**
     * 是否可以打印log<p/>
     *
     * @return isLoggable;
     */
    private static boolean isLoggable() {
        return isEnable;
    }

    /**
     * 获取默认tag，如果tag为null或空字符串，返回调用处类名
     *
     * @return tag
     */
    private static String getDefaultTag() {
        if (tag == null || tag.isEmpty()) {
            return getClassName();
        }

        return tag;
    }

    /**
     * 私有构造函数，并且L类不能被初始化
     */
    private L() {
        throw new UnsupportedOperationException("日志类L不能被初始化");
    }

    /**
     * 打印Verbose （冗余，全部的）类型的日志；
     *
     * @param tag tag
     * @param log log
     */
    public static void v(String tag, String log) {

        if (isLoggable()) {
            Log.v(tag, log);
        }
    }

    /**
     * 打印Verbose （冗余，全部的）类型的日志；
     *
     * @param log log
     */
    public static void v(String log) {

        if (isLoggable()) {
            Log.v(getDefaultTag(), log);
        }
    }

    /**
     * 打印Debug （调试）类型的日志；
     *
     * @param tag tag
     * @param log log
     */
    public static void d(String tag, String log) {

        if (isLoggable()) {
            Log.d(tag, log);
        }
    }

    /**
     * 打印Debug （调试）类型的日志；
     *
     * @param log log
     */
    public static void d(String log) {

        if (isLoggable()) {
            Log.d(getDefaultTag(), log);
        }
    }

    /**
     * 打印Info类型的日志；
     *
     * @param tag tag
     * @param log log
     */
    public static void i(String tag, String log) {

        if (isLoggable()) {
            Log.i(tag, log);
        }
    }

    /**
     * 打印Info类型的日志；
     *
     * @param log log
     */
    public static void i(String log) {

        if (isLoggable()) {
            Log.i(getDefaultTag(), log);
        }
    }

    /**
     * 打印Warn（警告）类型的日志；
     *
     * @param tag tag
     * @param log log
     */
    public static void w(String tag, String log) {

        if (isLoggable()) {
            Log.w(tag, log);
        }
    }

    /**
     * 打印Warn（警告）类型的日志；
     *
     * @param log log
     */
    public static void w(String log) {

        if (isLoggable()) {
            Log.w(getDefaultTag(), log);
        }
    }

    /**
     * 打印Error（错误）类型的日志；
     *
     * @param tag
     * @param log log
     */
    public static void e(String tag, String log) {

        if (isLoggable()) {
            Log.e(tag, log);
        }
    }

    /**
     * 打印Error（错误）类型的日志；
     *
     * @param log log
     */
    public static void e(String log) {

        if (isLoggable()) {
            Log.e(getDefaultTag(), log);
        }
    }

    /**
     * 打印Wtf（what a terrible failure）类型的日志；
     *
     * @param tag
     * @param log log
     */
    public static void wtf(String tag, String log) {
        if (isLoggable()) {
            Log.wtf(tag, log);
        }
    }

    /**
     * 打印Wtf（what a terrible failure）类型的日志；
     *
     * @param log log
     */
    public static void wtf(String log) {
        if (isLoggable()) {
            Log.wtf(getDefaultTag(), log);
        }
    }

    /**
     * 格式化json字符串并打印；
     *
     * @param json String
     */
    public static void json(String json) {
        if (TextUtils.isEmpty(json)) {
            e("警告：json值为空！");
            printCallHierarchy();
            return;
        }
        try {
            if (json.startsWith("{")) {
                JSONObject jsonObject = new JSONObject(json);
                String message = jsonObject.toString(JSON_INDENT);
                d(message);
                return;
            }
            if (json.startsWith("[")) {
                JSONArray jsonArray = new JSONArray(json);
                String message = jsonArray.toString(JSON_INDENT);
                d(message);
            }
        } catch (JSONException e) {
            e(e.getCause().getMessage() + "\n" + json);
        }
    }

    /**
     * 格式化xml字符串并打印
     *
     * @param xml xml内容
     */
    public static void xml(String xml) {
        if (TextUtils.isEmpty(xml)) {
            e("警告：xml值为空！");
            printCallHierarchy();
            return;
        }
        try {
            Source xmlInput = new StreamSource(new StringReader(xml));
            StreamResult xmlOutput = new StreamResult(new StringWriter());
            Transformer transformer = TransformerFactory.newInstance()
                    .newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty(
                    "{http://xml.apache.org/xslt}indent-amount", "2");
            transformer.transform(xmlInput, xmlOutput);
            d(xmlOutput.getWriter().toString().replaceFirst(">", ">\n"));
        } catch (TransformerException e) {
            e(e.getCause().getMessage() + "\n" + xml);
        }
    }

    /**
     * 打印调用类名以及位置
     */
    public static void print() {
        if (isLoggable()) {
            String tag = getClassName();
            String method = getMethodAndLine();
            Log.v(tag, method);
        }
    }

    /**
     * 打印对象
     *
     * @param object 需要打印的对象
     */
    public static void print(Object object) {
        if (isLoggable()) {
            String tag = getClassName();
            String method = getMethodAndLine();
            String content = "";
            if (object != null) {
                content = object.toString() + " :=====> " + method;
            } else {
                content = "错误！object 值为null ：=====>" + method;
            }
            Log.d(tag, content);
        }
    }

    /**
     * 打印调用层级
     */
    public static void printCallHierarchy() {
        if (isLoggable()) {
            String tag = getClassName();
            String method = getMethodAndLine();
            String hierarchy = getCallHierarchy();
            Log.v(tag, method + hierarchy);
        }
    }

    /**
     * 获取调用层级关系
     *
     * @return callHierarchy
     */
    private static String getCallHierarchy() {
        String result = "";
        StackTraceElement[] trace = (new Exception()).getStackTrace();
        for (int i = 2; i < trace.length; i++) {
            result += "\r\t" + trace[i].getClassName() + "." + trace[i]
                    .getMethodName() + "():" + trace[i].getLineNumber();
        }
        return result;
    }

    /**
     * 获取调用处类名
     *
     * @return className
     */
    private static String getClassName() {
        String result = "";
        StackTraceElement thisMethodStack = (new Exception())
                .getStackTrace()[2];
        result = thisMethodStack.getClassName();
        return result;
    }

    /**
     * 获取代码调用位置（可以双击跳转到源码位置）
     *
     * @return result
     */
    private static String getMethodAndLine() {
        String result = "at ";
        StackTraceElement thisMethodStack = (new Exception())
                .getStackTrace()[2];
        result += thisMethodStack.getClassName() + ".";
        result += thisMethodStack.getMethodName();
        result += "(" + thisMethodStack.getFileName();
        result += ":" + thisMethodStack.getLineNumber() + ")  ";
        return result;
    }

}