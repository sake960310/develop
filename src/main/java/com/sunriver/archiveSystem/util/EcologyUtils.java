package com.sunriver.archiveSystem.util;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

import java.io.File;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.Timestamp;
import java.util.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @program: archiveSystem
 * @description: 泛微E9工具类
 * @author: wh
 * @create: 2024-04-29 09:59
 */
public class EcologyUtils {
    //工具类
    //excel工具类
    static ExcelUtils excelUtils = new ExcelUtils();
    static JDBCUtils jdbcUtils = new JDBCUtils();
    private static final String baseTableFields = excelUtils.getBaseTableConf();
    private static final String baseTable = excelUtils.getBaseTableName();

    public static void main(String[] args) {
        List<String> strings = List.of("1111", "2222", "3213", "123");
        System.out.println(JSON.toJSONString(strings));
    }

    /**
     *  E9插入档案数据
     * @param excelDataMap 附件读取data
     * @param exportType    导入类型
     */
    public JSONObject insertArchiveData(String exportType,Map<String, Object> excelDataMap){
        List<String> headers = (List<String>) excelDataMap.get("headers");
        System.out.println("excelDataMap::标题数据："+headers.toString());
        List<List<Object>> datas = (List<List<Object>>) excelDataMap.get("data");
        System.out.println("excelDataMap::数据一共行数"+datas.size());

        Map<String,Object> map = excelUtils.getExportConf(exportType);
        String tableName = map.get("tableName").toString();
        //导入数据字段keyList   将headersList 转换成数据库字段
        List<String> headersList = new ArrayList<>();
        Map<String, String> fieldNameMap = jdbcUtils.getFieldName(tableName, headers);
        for (String labelName : headers) {
            headersList.add(fieldNameMap.get(labelName));
        }
        System.out.println("上传附件的头部headersList=="+JSON.toJSONString(headersList));
        //配置文件档案数据选择框字段List
        List<String> selectList = (List<String>) map.get("listSelect");
        //记录选择框数据下标
        StringBuilder indexNum = new StringBuilder(",");
        //获取选择框键值对
        Map<String,Map<String,Integer>> selectMap = new HashMap<>();
        //获取导入档案选择框字段位置及实际值
        if(!selectList.isEmpty()){
            Connection conn = JDBCUtils.getConnection();
            for (String selectKey : selectList) {
                indexNum.append(headersList.indexOf(selectKey)).append(",");
                selectMap.put(selectKey, jdbcUtils.getSelectActValue(conn, tableName, selectKey));
            }
            JDBCUtils.closeAll(null,null,conn);
        }
        indexNum = new StringBuilder(",".contentEquals(indexNum) ? "" : indexNum.toString());
        System.out.println("indexNum==="+indexNum);
        return insertDataAction(exportType,tableName,headersList,datas,indexNum.toString(),selectMap,selectList,tableName.substring(tableName.indexOf("uf_")+3).toLowerCase());
    }

    /**
     *restful接口调用案例
     *  E9建模档案数据接口执行
     */
    public JSONObject insertDataAction(String exportType,String tableName,List<String> headers, List<List<Object>> data, String indexNum,Map<String,Map<String,Integer>> selectMap,List<String> selectList,String archiveApiName){
        long startTime = System.currentTimeMillis(); // 记录程序结束时间
        //封装datajson.data参数
        Iterator<List<Object>> dataIterator = data.iterator();
        //分批次执行Action
        List<Map<String, Object>> batchDataList = new ArrayList<>();
        int batchSize = 5;
        StringBuilder resultStringBuilder = new StringBuilder();
        //成功与失败数据
        JSONArray failDataArray =  new JSONArray();
        JSONArray baseDataArray = new JSONArray();
        JSONArray failBaseDataArray = new JSONArray();
        //档号与档案ID关联关系
        Map<String, String> resultRelationMap = new HashMap<>();
        while(dataIterator.hasNext()){
            List<Object> datum = dataIterator.next();
            Map<String,Object> datajson = new HashMap<>();
            JSONObject mainTable = new JSONObject();
            for (int i = 0; i < headers.size(); i++) {
                String key = headers.get(i);
                String value = datum.get(i).toString();
                if (!"".equals(indexNum) && indexNum.contains("," + i + ",")) {
                    int actValue = selectMap.get(key).get(value);
                    mainTable.put(key, actValue+"");
                } else {
                    mainTable.put(key, value);
                }
            }
            datajson.put("mainTable", mainTable);
            //operationinfo
            datajson.put("operationinfo", getPostOperationInfo("1"));
            batchDataList.add(datajson);
            if (batchDataList.size() == batchSize || !dataIterator.hasNext()) {
                try {
                    long startdataTime = System.currentTimeMillis(); // 记录程序结束时间
                    resultStringBuilder.append(this.doAction(archiveApiName, batchDataList));
                    long endTime = System.currentTimeMillis(); // 记录程序结束时间
                    long executionBaseDataTime = (endTime - startdataTime)/1000; // 计算程序执行时间
                    System.out.println("档案数据导入Action执行花费了 " + executionBaseDataTime + " 秒");
                } catch (Exception e) {
                    e.printStackTrace();
                }
                batchDataList.clear();
                //记录成功数据和失败数据
                String resultString = resultStringBuilder.toString();
                System.out.println("insertDataAction()--resultString=="+resultString);
                resultStringBuilder.setLength(0);
                //获取档号与档案关联关系
                resultRelationMap.putAll(getResultRelationMap(resultString));
                // 获取成功数据和失败数据的数组
                JSONObject innerObject = explainEcologResult(resultString);
                JSONArray dataArray = innerObject.getJSONArray("data");
                failDataArray.addAll(innerObject.getJSONArray("faildata"));
                //基表数据插入
                if(!dataArray.isEmpty()){
                    long startBaseTime = System.currentTimeMillis(); // 记录程序结束时间
                    String baseResultString = insertBaseDataAction(exportType,tableName,dataArray);
                    long endBaseTime = System.currentTimeMillis(); // 记录程序结束时间
                    long executionBaseDataTime = (endBaseTime - startBaseTime)/1000; // 计算程序执行时间
                    System.out.println("基表数据导入Action执行花费了 " + executionBaseDataTime + " 秒");
                    JSONObject baseJsonObject = explainEcologResult(baseResultString);
                    // 获取成功数据和失败数据的数组
                    baseDataArray.addAll(baseJsonObject.getJSONArray("data"));
                    failBaseDataArray.addAll(baseJsonObject.getJSONArray("faildata"));
                    dataArray.clear();
                }else{
                    System.out.println("基表无可插入数据");
                }
            }
        }

        String resultString = resultStringBuilder.toString();
        //记录执行结果
        JSONObject resultJsonObject = new JSONObject();
        resultJsonObject.put("resultString",resultString);
        long endTime = System.currentTimeMillis(); // 记录程序结束时间
        long executionDataTime = (endTime - startTime)/1000; // 计算程序执行时间
        System.out.println("档案数据导入Action执行花费了 " + executionDataTime + " 秒");
        //生成失败附件
        String failFileName = "";
        if(!failDataArray.isEmpty()){
            failFileName = excelUtils.createFailFile(exportType,failDataArray,selectList,selectMap);
        }
        long endFileTime = System.currentTimeMillis(); // 记录程序结束时间
        long executionFileTime = (endFileTime - endTime)/1000; // 计算程序执行时间
        System.out.println("生成失败文件执行花费了 " + executionFileTime + " 秒");
        resultJsonObject.put("failDataName",failFileName);
        resultJsonObject.put("successData",resultRelationMap.size());
        resultJsonObject.put("successBaseData",baseDataArray.size());
        resultJsonObject.put("failBaseData",failBaseDataArray.size());
        resultJsonObject.put("failData",failDataArray.size());
        resultJsonObject.put("resultRelationMap",resultRelationMap);

        return resultJsonObject;
    }

    /**
     * restful接口调用案例
     * E9建模档案基表数据接口执行
     *
     * @return 接口返回结果
     */
    public String insertBaseDataAction(String exportType,String tableName, JSONArray dataArray){
        List<Map<String,Object>> baseDataList = new ArrayList<>();
        Connection connection = JDBCUtils.getConnection();
        StringBuilder baseTableTmp = new StringBuilder(",");
        for (String str : baseTableFields.split(",")) {
            baseTableTmp.append(str.substring(str.indexOf("_"))).append(",");
        }
        Map<String,String> viewMap = jdbcUtils.getArchiveViewData(connection,exportType);
        //封装datajson.data参数
        for (int i = 0; i < dataArray.size(); i++) {
            //基表
            Map<String,Object> baseDatajson = new HashMap<>();
            JSONObject baseMainTable = new JSONObject();
            JSONObject item = dataArray.getJSONObject(i);
            String billId = item.getString("billid");
            String originaldata = item.getString("originaldata");
            JSONObject mainTable = JSON.parseObject(originaldata).getJSONObject("mainTable");
            for (Map.Entry<String, Object> entry:mainTable.entrySet()){
                //构建基表数据
                if(baseTableTmp.toString().contains(entry.getKey())){
                    baseMainTable.put(entry.getKey(), entry.getValue());
                }
            }
            //基表数据补充
            baseMainTable.put("aid", billId);
            baseMainTable.put("mid", viewMap.get("mid"));
            baseMainTable.put("fid", viewMap.get("fid"));
            baseMainTable.put("archiveTable", tableName);

            baseDatajson.put("mainTable", baseMainTable);
            baseDatajson.put("operationinfo", getPostOperationInfo("1"));
            baseDataList.add(baseDatajson);
        }
        JDBCUtils.closeAll(null,null,connection);
        return this.doAction(baseTable.substring(baseTable.indexOf("uf_")+3).toLowerCase(),baseDataList);
    }

    /**
     *  导入档案附件
     * @param data 数据
     * @param archiveApiName 接口API地址
     */
    public void exportFileAction(Map<String,String> data,String archiveApiName){
        List<Map<String,Object>> dataList = new ArrayList<>();
        JSONObject mainTable = new JSONObject();
        Map<String,Object> datajson = new HashMap<>();
        //封装datajson.data参数
        mainTable.putAll(data);
        datajson.put("mainTable", mainTable);
        //operationinfo
        datajson.put("operationinfo", getPostOperationInfo("1"));
        dataList.add(datajson);
        this.doAction(archiveApiName,dataList);
    }

    /**
     *  执行接口请求
     * @param archiveApiName 档案接口标识
     * @param dataList 接口data参数
     */
    public String doAction(String archiveApiName,List<Map<String,Object>> dataList){
        ExcelUtils excelUtils = new ExcelUtils();
        //获取Ecology配置信息
        Map<String,String> ecologyConf = excelUtils.getEcologyConf();

        CloseableHttpResponse response;// 响应类,
        CloseableHttpClient httpClient = HttpClients.createDefault();
        //restful接口url
        String url = ecologyConf.get("oaPath")+"/api/cube/restful/interface/saveOrUpdateModeData/insertArchiveDataService_"+archiveApiName;
        //当前日期
        String currentDate = getCurrentDate();
        //当前时间
        String currentTime = getCurrentTime();
        //获取时间戳
        Map<String,Object> params = new HashMap<>();
        Map<String,Object> paramDatajson = new HashMap<>();
        //data
        paramDatajson.put("data",dataList);
        //header
        paramDatajson.put("header",getPostHeader(ecologyConf.get("systemid"),ecologyConf.get("password")));
        params.put("datajson",paramDatajson);
        //restful接口url
        HttpPost httpPost = new HttpPost(url);
        //装填参数
        List nvps = new ArrayList();
        if(params!=null){
            for (Object entry : params.entrySet()) {
                Map.Entry map = (Map.Entry)entry;
                nvps.add(new BasicNameValuePair((String) map.getKey(), JSONObject.toJSONString(map.getValue())));
            }
        }
        String resulString = "";
        try{
            httpPost.addHeader("Content-Type","application/x-www-form-urlencoded; charset=utf-8");
            httpPost.setEntity(new UrlEncodedFormEntity(nvps, "UTF-8"));
            response = httpClient.execute(httpPost);
            if (response != null && response.getEntity() != null) {
                //返回信息
                resulString = EntityUtils.toString(response.getEntity());
                //todo这里处理返回信息
//                System.out.println("成功:"+ resulString);
            }else{
                System.out.println("获取数据失败，请查看日志:"+currentDate+" "+currentTime);
            }
        }catch (Exception e){
            System.out.println("请求失败"+currentDate+" "+currentTime+"====errormsg:"+e.getMessage());
        }
        return resulString;
    }

    /**
     * 解析api结果
     * @param resultString E9返回结果
     * @return 返回JsonObject
     */
    public JSONObject explainEcologResult(String resultString){
        // 解析外部 JSON 字符串
        JSONObject outerObject = JSON.parseObject(resultString);
        // 获取内部的 JSON 字符串
        String innerJsonString = outerObject.getString("datajson");
        // 解析内部 JSON 字符串
        return JSON.parseObject(innerJsonString);
    }

    public Map<String, String> getResultRelationMap(String resultString){
        //档案附件_压缩文件处理
        JSONArray dataArray = explainEcologResult(resultString).getJSONArray("data");
        Map<String, String> relationMap = new HashMap<>();
        for (int i = 0; i < dataArray.size(); i++) {
            //基表
            JSONObject item = dataArray.getJSONObject(i);
            String billId = item.getString("billid");
            String originaldata = item.getString("originaldata");
            JSONObject mainTable = JSON.parseObject(originaldata).getJSONObject("mainTable");
            for (Map.Entry<String, Object> entry : mainTable.entrySet()) {
                //查询档号
                if ("DH".equals(entry.getKey())) {
                    relationMap.put(entry.getValue().toString(), billId);
                }
            }
        }
        return relationMap;
    }
    /**
     *  生成请求header
     * @param systemid 系统标识
     * @param password 密码
     */
    public Map<String,String> getPostHeader(String systemid,String password){
        //header
        //获取时间戳
        String currentTimeTamp = getTimestamp();
        Map<String,String> header = new HashMap<>();
        //封装header里的参数
        header.put("systemid",systemid);
        header.put("currentDateTime",currentTimeTamp);
        String md5Source = systemid+password+currentTimeTamp;
        String md5OfStr = getMD5Str(md5Source).toLowerCase();
        //Md5是：系统标识+密码+时间戳 并且md5加密的结果
        header.put("Md5",md5OfStr);
        return header;
    }

    //生成请求operationinfo
    public JSONObject getPostOperationInfo(String userId){
        //封装operationinfo参数
        JSONObject operationinfo = new JSONObject();
        operationinfo.put("operator", userId);
        return operationinfo;
    }

    public String getMD5Str(String plainText){
        //定义一个字节数组
        byte[] secretBytes = null;
        try {
            // 生成一个MD5加密计算摘要
            MessageDigest md = MessageDigest.getInstance("MD5");
            //对字符串进行加密
            md.update(plainText.getBytes());
            //获得加密后的数据
            secretBytes = md.digest();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("没有md5这个算法！");
        }
        //将加密后的数据转换为16进制数字
        StringBuilder md5code = new StringBuilder(new BigInteger(1, secretBytes).toString(16));
        // 如果生成数字未满32位，需要前面补0
        for (int i = 0; i < 32 - md5code.length(); i++) {
            md5code.insert(0, "0");
        }
        return md5code.toString();
    }

    public static String getCurrentTime() {
        Date newdate = new Date();
        long datetime = newdate.getTime();
        Timestamp timestamp = new Timestamp(datetime);
        return (timestamp.toString()).substring(11, 13) + ":" + (timestamp.toString()).substring(14, 16) + ":"
                + (timestamp.toString()).substring(17, 19);
    }

    public static String getCurrentDate() {
        Date newdate = new Date();
        long datetime = newdate.getTime();
        Timestamp timestamp = new Timestamp(datetime);
        return (timestamp.toString()).substring(0, 4) + "-" + (timestamp.toString()).substring(5, 7) + "-"
                + (timestamp.toString()).substring(8, 10);
    }

    /**
     * 获取当前日期时间。 YYYY-MM-DD HH:MM:SS
     * @return		当前日期时间
     */
    public static String getCurDateTime() {
        Date newdate = new Date();
        long datetime = newdate.getTime();
        Timestamp timestamp = new Timestamp(datetime);
        return (timestamp.toString()).substring(0, 19);
    }

    /**
     * 获取时间戳   格式如：19990101235959
     */
    public static String getTimestamp(){
        return getCurDateTime().replace("-", "").replace(":", "").replace(" ", "");
    }
}
