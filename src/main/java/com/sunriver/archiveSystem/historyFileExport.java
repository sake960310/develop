package com.sunriver.archiveSystem;

import com.sunriver.archiveSystem.util.ExcelUtils;
import com.sunriver.archiveSystem.util.JDBCUtils;

import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;
import java.io.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

@WebServlet("/hisexport")
@MultipartConfig
public class historyFileExport extends HttpServlet {

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        request.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType("text/html; charset="+StandardCharsets.UTF_8.name());
        //工具类
        ExcelUtils excelUtils = new ExcelUtils();
        JDBCUtils jdbcUtils = new JDBCUtils();
        Connection connection = JDBCUtils.getConnection();
        Map<String, String> exportTypeMap = excelUtils.getExportTypeMap();
        //获取请求参数
        Part fileData = request.getPart("fileData");//档案基础数据
        InputStream fileContent = fileData.getInputStream();
        //1.获取附件内容
        Map<String, Object> excelDataMap = excelUtils.readExcel(fileContent);
        @SuppressWarnings("unchecked")
        List<List<Object>> data = (List<List<Object>>) excelDataMap.get("data");
        Iterator<List<Object>> dataItr = data.iterator();
        while(dataItr.hasNext()){
            List<Object> row = dataItr.next();
            String exportType = "";//档案类型
            String DH = "";//档号
            String filePath = "";//附件地址
            String fileTitle = "";//附件名
            String fileName = "";//存储名
            String fileType = "";//附件类型
            for (int i = 0; i < row.size(); i++) {
                switch (i) {
                    case 0:exportType = row.get(i).toString();break;
                    case 1:DH = row.get(i).toString();break;
                    case 2:filePath = row.get(i).toString();break;
                    case 3:fileTitle = row.get(i).toString();break;
                    case 4:fileName = row.get(i).toString();break;
                    case 5:fileType = row.get(i).toString();break;
                }
            }
            //2.解析附件
            File filePathTmp = new File(excelUtils.getSharePath()+File.separator+filePath);
            File file = new File(filePathTmp+File.separator+fileName);
            //3.建立关联
            jdbcUtils.establishRelArchiveFile(connection,exportType,exportTypeMap.get(exportType),DH,filePathTmp.toString().replace(new File(excelUtils.getSharePath())+"",""),fileTitle+"."+fileType,fileName,excelUtils.getFileSize(file.length()));
        }
        JDBCUtils.closeAll(null,null,connection);
        response.getWriter().write("success");
    }

    public static void main(String[] args) {
        File file = new File("\\\\\\\\172.21.21.40\\\\数科\\\\filesystem\\\\2024\\\\5\\\\20240514105458104_文书档案_202405081.xlsx");
        System.out.println(file.getAbsolutePath());
        System.out.println(file.exists());
        System.out.println(file.length());

    }
}
