package com.sunriver.archiveSystem;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.sunriver.archiveSystem.util.EcologyUtils;
import com.sunriver.archiveSystem.util.ExcelUtils;

@WebServlet("/upload")
@MultipartConfig
public class FileUploadServlet extends HttpServlet {

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        request.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType("text/html; charset="+StandardCharsets.UTF_8.name());
        long startTime = System.currentTimeMillis(); // 记录程序开始时间
        //工具类
        ExcelUtils excelUtils = new ExcelUtils();
        EcologyUtils ecologyUtils = new EcologyUtils();
        //获取请求参数
        Part fileData = request.getPart("fileData");//档案基础数据
        Part archiveFile = request.getPart("archiveFile");//档案附件数据
        String exportType = request.getParameter("exportType");
        String exportTableName = excelUtils.getExportTypeMap().get(exportType);
        // 原始名称
        String originalFileName = fileData.getSubmittedFileName();
        //档案数据_文件上传
        excelUtils.saveFileToNAS(excelUtils.getSharePath(),fileData.getInputStream(),originalFileName);
        //档案数据处理
        Map<String, Object> excelDataMap = excelUtils.readExcel(fileData.getInputStream(),exportType);
        if(excelDataMap.get("failMode") != null){
            response.getWriter().write(new JSONObject(excelDataMap).toString());
        }else{
            long endExcelTime = System.currentTimeMillis(); // 记录程序结束时间
            long executionExcelDataTime = (endExcelTime - startTime)/1000; // 计算程序执行时间
            System.out.println("excel解析程序执行花费了 " + executionExcelDataTime + " 秒");

            JSONObject resultJsonObject = ecologyUtils.insertArchiveData(exportType,excelDataMap);
            long endDataTime = System.currentTimeMillis(); // 记录程序结束时间
            long executionDataTime = (endDataTime - endExcelTime)/1000; // 计算程序执行时间
            System.out.println("数据导入程序执行花费了 " + executionDataTime + " 秒");
            //档案附件_压缩文件处理
            Map relationMap = resultJsonObject.getObject("resultRelationMap",Map.class);
            if(archiveFile.getSize() > 0  && !relationMap.isEmpty()){
                excelUtils.exportArchiveFile(archiveFile.getInputStream(),new File(excelUtils.getSharePath()+File.separator+excelUtils.getExportFilePath()),exportType,exportTableName,relationMap);
            }
//        if(archiveFile.getSize() > 0){
//            excelUtils.exportArchiveFile(archiveFile.getInputStream(),new File(excelUtils.getSharePath()+File.separator+excelUtils.getExportFilePath()),exportType,exportTableName);
//        }
            long endTime = System.currentTimeMillis(); // 记录程序结束时间
            long executionTime = (endTime - endDataTime)/1000; // 计算程序执行时间
            System.out.println("附件程序执行花费了 " + executionTime + " 秒");
            long totalTime = (endTime - startTime)/1000; // 计算程序执行时间
            System.out.println("全程 执行花费了 " + totalTime + " 秒");
            response.getWriter().write(String.valueOf(resultJsonObject));
        }

        //response.sendRedirect("http://172.21.61.169:8080/archiveSystem/jsp/upload.jsp");
    }
}
