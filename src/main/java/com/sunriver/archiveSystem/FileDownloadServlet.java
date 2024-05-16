package com.sunriver.archiveSystem;

import com.sunriver.archiveSystem.util.ExcelUtils;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

@WebServlet("/download")
@MultipartConfig
public class FileDownloadServlet extends HttpServlet {

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        request.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType("text/html; charset="+StandardCharsets.UTF_8.name());
        //工具类
        ExcelUtils excelUtils = new ExcelUtils();
        //获取请求参数
        String isHistory = request.getParameter("isHistory");//1 下载历史导入模版  2.下载错误信息
        File file = null;
        if("1".equals(isHistory)){
            file = new File(excelUtils.getSharePath()+File.separator+excelUtils.getImportTemplatePath()+File.separator+"历史档案附件关联模版.xlsx");
        }else{
            String exportType = request.getParameter("downloadType");
            System.out.println("exportType:"+exportType+",,,isHistory"+isHistory);
            exportType = exportType.substring(0,exportType.lastIndexOf("_"));
            file = new File(excelUtils.getSharePath()+File.separator+excelUtils.getImportTemplatePath()+File.separator+exportType+"_模板.xlsx");
            if(!file.exists()){
                //初始化模版
                excelUtils.createMode(exportType,file,new ArrayList<>(),null);
            }
        }
        //2.1用字节流关联
        FileInputStream fis = new FileInputStream(file);

        String filename = URLEncoder.encode(file.getName(), StandardCharsets.UTF_8);//防止文件名中有中文乱码
        response.setHeader("Content-Disposition","attachment;filename="+filename);

        FileInputStream input = new FileInputStream(file);
        BufferedInputStream bis = new BufferedInputStream(input);
        ServletOutputStream sos = response.getOutputStream();

        byte[] buffer = new byte[1024];
        int len=0;
        while((len=bis.read(buffer, 0, 1024))!=-1){
            sos.write(buffer, 0, len);
        }
        bis.close();
        fis.close();

//        response.getWriter().println("File uploaded and excel content extracted successfully!");
//        response.sendRedirect("http://localhost:8080/demo2/jsp/upload.jsp");
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        request.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType("text/html; charset="+StandardCharsets.UTF_8.name());
        //工具类
        ExcelUtils excelUtils = new ExcelUtils();
        //获取请求参数
        String fileName = request.getParameter("fileName");//1 下载历史导入模版  2.下载错误信息
        File file = new File(excelUtils.getSharePath()+File.separator+"failData"+File.separator+fileName);
        //2.1用字节流关联
        FileInputStream fis = new FileInputStream(file);
        String filename = URLEncoder.encode(file.getName(), StandardCharsets.UTF_8);//防止文件名中有中文乱码
        response.setHeader("Content-Disposition","attachment;filename="+filename);

        FileInputStream input = new FileInputStream(file);
        BufferedInputStream bis = new BufferedInputStream(input);
        ServletOutputStream sos = response.getOutputStream();

        byte[] buffer = new byte[1024];
        int len=0;
        while((len=bis.read(buffer, 0, 1024))!=-1){
            sos.write(buffer, 0, len);
        }
        bis.close();
        fis.close();
        file.delete();
    }
}
