package com.sunriver.archiveSystem.util;

import java.io.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.sql.Connection;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/**
 * Excel解析工具类
 */
public class ExcelUtils {
//    private static final Logger logger = LogManager.getLogger(ExcelUtils.class);
    //导入配置文件
    private static final String EXPORTCONF_FILE_PATH = "exportConf.properties";
    private static final Properties prop = new Properties();
    static{
        try {
            prop.load(new InputStreamReader(Objects.requireNonNull(ExcelUtils.class.getClassLoader().getResourceAsStream(EXPORTCONF_FILE_PATH)), StandardCharsets.UTF_8));
        }catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     *  解析Excel
     * @param fileContent 文件内容
     * @return 内容map
     */
    public Map<String, Object> readExcel(InputStream fileContent,String exportType) throws IOException {
        // 创建一个Map来存储Excel的表头、表头列数和表格数据内容
        Map<String, Object> excelData = new HashMap<>();
        // Excel表头
        List<String> headers = new ArrayList<>();
        // 表格数据内容
        List<List<Object>> rowData = new ArrayList<>();
        //解析excel
        XSSFWorkbook workbook = new XSSFWorkbook(fileContent);
        Iterator<Row> rowIterator = workbook.getSheetAt(0).iterator();
        // Skip the first row (title row)
        //表头列数
        int titleRow = 0;
        if (rowIterator.hasNext()) {
            Row row = rowIterator.next();
//            int rowNum = row.getRowNum()+1;
            titleRow = row.getLastCellNum();
//            System.out.println("表头列："+titleRow);
            for (int i = 0; i < row.getLastCellNum(); i++) {
                Cell cell =  row.getCell(i);
//                int colNum = cell.getColumnIndex()+1;
                if(cell.getCellType() == CellType.STRING && !cell.getStringCellValue().isEmpty()){
//                        System.out.println("第"+rowNum+"行，cell::"+colNum+"列："+cell.getStringCellValue());
                    //表头信息记录
                    headers.add(cell.getStringCellValue());

                }
            }
        }
        //校验模版是否正确
        if(!exportType.isEmpty()){
            Map<String, Object> exportConf = getExportConf(exportType);
            List<String> headersConfList = (List<String>) exportConf.get("list");
            String tableName = (String) exportConf.get("tableName");
            Map<String, String> fieldLabelNameMap = new JDBCUtils().getFieldLabelName(tableName, headersConfList);
            List<String> headersList = new ArrayList<>(fieldLabelNameMap.values());
            boolean isEqual = headersList.size() == headers.size() && new HashSet<>(headersList).containsAll(headers);
            if (!isEqual){
                System.out.println("使用了错误导入模版！");
                excelData.put("failMode","1");
                workbook.close();
                fileContent.close();
                return excelData;
            }
        }

        while (rowIterator.hasNext()) {
            Row row = rowIterator.next();
//            int rowNum = row.getRowNum()+1;
            List<Object> rows = new ArrayList<>();
            for (int i = 0; i < titleRow; i++) {
                Cell cell = row.getCell(i);
                if(cell != null){
                    CellType type = cell.getCellType();
                    if (type == CellType.STRING) {
//                    System.out.println("第"+rowNum+"行，cell::"+colNum+"列："+cell.getStringCellValue());
                        rows.add(cell.getStringCellValue());
                    } else if (type == CellType.NUMERIC) {
//                    System.out.println("第"+rowNum + "行，cell::" + colNum + "列：" + (int)Math.floor(cell.getNumericCellValue()));
                        rows.add((int) Math.floor(cell.getNumericCellValue()));
                    } else if (type == CellType.BLANK) {
                        rows.add("");
                    }
                }else{
                    rows.add("");
                }
            }
            rowData.add(rows);
        }
        // 将表头、表头列数和表格数据内容放入Map中
        excelData.put("headers", headers);
        excelData.put("columnCount", headers.size());
        excelData.put("data", rowData);
        workbook.close();
        fileContent.close();
        return excelData;
    }

    /**
     * 生成附件
     * @param newFile   文件地址
     * @param sheetName  表名
     * @param titles    标题列
     * @param data       数据列
     */
    public File generateExcelDocument(File newFile, String sheetName, List<String> titles, List<List<Object>> data,String dataIndexs) throws IOException {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet(sheetName);
        // 写入标题
        Row titleRow = sheet.createRow(0);
        for (int i = 0; i < titles.size(); i++) {
            Cell cell = titleRow.createCell(i);
            //创建样式
            CellStyle style =  workbook.createCellStyle();
            //设置字体
            Font font = workbook.createFont();
            font.setFontName("Arial");
            font.setFontHeightInPoints((short)12);
            font.setBold(true);
            style.setFont(font);
            //设置背景颜色
            style.setFillForegroundColor(IndexedColors.YELLOW.getIndex());
            style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            //居中
            style.setAlignment(HorizontalAlignment.CENTER);
            //边框
            style.setBorderBottom(BorderStyle.THIN);
            style.setBorderLeft(BorderStyle.THIN);
            style.setBorderTop(BorderStyle.THIN);
            style.setBorderRight(BorderStyle.THIN);
            //应用样式到单元格
            cell.setCellStyle(style);
            cell.setCellValue(titles.get(i));
        }
        if(data!=null){
            LocalDate epochDate = LocalDate.of(1899, 12, 30);
            // 写入表头
            for (int i = 0; i < data.size(); i++) {
                Row row = sheet.createRow(i + 1);
                for (int j = 0; j < titles.size(); j++) {
                    Cell cell = row.createCell(j);
                    String value = data.get(i).get(j).toString();
                    //日期转换
                    if(dataIndexs.contains(","+j+",") && value.matches("\\d+")){
                        cell.setCellValue(epochDate.plusDays(Long.parseLong(value))+"");
                    }else{
                        cell.setCellValue(value);
                    }
                }
            }
        }
        // 保存Excel文件
        try (FileOutputStream outputStream = new FileOutputStream(newFile)) {
            workbook.write(outputStream);
            System.out.println("Excel文件已生成: " + newFile);
        } catch (IOException e) {
            System.err.println("生成Excel文件时出错: " + e.getMessage());
        }
        workbook.close();
        return newFile;
    }

    /**
     * 获取导入配置文件内容
     */
    public Map<String,Object> getExportConf(String lx){
        Map<String,Object> map = new HashMap<>();
        String exportJson = prop.getProperty("export");
        String modeFilePath = prop.getProperty("modeFilePath");
        Map<String,String> ecConf = new HashMap<>();
        ecConf.put("oaPath",prop.getProperty("oaPath"));
        ecConf.put("systemid",prop.getProperty("systemid"));
        ecConf.put("password",prop.getProperty("password"));
        ObjectMapper mapper = new ObjectMapper();
        Map<String,List<String>> mapTemp;
        try {
            mapTemp = mapper.readValue(exportJson, HashMap.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        List<String> listText = new ArrayList<String>();//普通文本
        List<String> listSelect = new ArrayList<String>();//选择框
        List<String> list = new ArrayList<String>();//转换后List
        int index = 0;
        Iterator<String> mapTempItr = mapTemp.get(lx).iterator();
        while (mapTempItr.hasNext()) {
            String field = mapTempItr.next();
            if(index == 0){
                map.put("tableName",field);
            }else if(field.contains("$1")){
                listSelect.add(field.replace("$1",""));
                list.add(field.replace("$1",""));
            } else{
                listText.add(field);
                list.add(field);
            }
            index++;
        }
        map.put("listText", listText);
        map.put("listSelect", listSelect);
        map.put("list", list);
        map.put("modeFilePath",modeFilePath);
        map.put("ecologyConf",ecConf);
        return map;
    }

    /**
     * 获取Ecology配置
     */
    public Map<String, String> getEcologyConf(){
        Map<String,String> map = new HashMap<>();
        map.put("oaPath",prop.getProperty("oaPath"));
        map.put("systemid",prop.getProperty("systemid"));
        map.put("password",prop.getProperty("password"));
        return map;
    }

    /**
     * 获取档案基表表名
     */
    public String getBaseTableName(){
        return prop.getProperty("baseTableName");
    }

    /**
     * 获取档案系统基表字段集合
     */
    public String getBaseTableConf(){
        return prop.getProperty("baseTableFields");
    }

    /**
     * 获取档案所有类型
     */
    public Map<String,String> getExportTypeMap(){
        String exportJson = prop.getProperty("export");
        ObjectMapper mapper = new ObjectMapper();
        Map<String,List<String>> mapTemp = null;
        try {
            mapTemp = mapper.readValue(exportJson, HashMap.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        //将配置文件中全部类型去除作为导入类型
        Map<String,String> exportTypeMap = new HashMap<>();
        for (Map.Entry<String,List<String>> entry : mapTemp.entrySet()){
            exportTypeMap.put(entry.getKey(),entry.getValue().get(0));
        }
        return exportTypeMap;
    }

    /**
     * 获取共享盘地址
     */
    public String getSharePath(){
        return prop.getProperty("networkSharePath");
    }

    /**
     * 获取导入文件地址
     */
    public String getExportFilePath(){
        return prop.getProperty("exportFilePath");
    }

    /**
     * 获取模版文件地址
     */
    public String getImportTemplatePath(){
        return prop.getProperty("importTemplatePath");
    }

    /**
     *  生成模板附件
     * @param lx 导入类型
     */
    public File createMode(String lx,File generateExcelFile,List<String> list,List<List<Object>> data){
        JDBCUtils jdbcUtils = new JDBCUtils();
        try {
            Map<String, Object> exportConf = this.getExportConf(lx);
            List<String> mapLabelName = new ArrayList<>();
            StringBuilder dataIndexs = new StringBuilder(",");
            if(list.isEmpty()){
                list = (List<String>) exportConf.get("list");
            }
            @SuppressWarnings("unchecked")
            Map<String,String> map = jdbcUtils.getFieldLabelName(exportConf.get("tableName").toString(),list);
            for (int i=0; i<list.size(); i++){
                String name = map.get(list.get(i).replace("$1", ""));
                mapLabelName.add(name);
                if(name.contains("日期")){
                    dataIndexs.append(i).append(",");
                }
            }
            // 生成Excel文档
            if(!generateExcelFile.exists()) {
                this.generateExcelDocument(generateExcelFile, "Sheet1", mapLabelName, data,dataIndexs.toString());
            }
            return generateExcelFile;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     *  生成失败数据文件
     * @param exportType 导入类型
     * @param failDataArray 失败文件数据集
     * @return 返回失败文件名
     */
    public String createFailFile(String exportType, JSONArray failDataArray,List<String> selectList,Map<String,Map<String,Integer>> selectMap){
        String fileName = "";
        //生成失败附件
        File newFile = new File(getSharePath()+File.separator+"failData"+File.separator+exportType+"_"+ EcologyUtils.getTimestamp()+".xlsx");
        List<List<Object>> failDataList = new ArrayList<>();
        List<String> mapLabelNameKey = new ArrayList<>();
        //选择框键值对互换位置
        Map<String, Map<String, String>> selectValKeyMap = swappedSelectMap(selectMap);
        for (int i = 0; i < failDataArray.size(); i++) {
            JSONObject item = failDataArray.getJSONObject(i);
            String originaldata = item.getString("originaldata");
            JSONObject mainTable = JSON.parseObject(originaldata).getJSONObject("mainTable");
            List<Object> datum = new ArrayList<>();
            for (String key : mainTable.keySet()) {
                if( i==0 ){
                    mapLabelNameKey.add(key);
                }
                //选择框实际值转换成显示值
                if(selectList.contains(key)){
                    datum.add(selectValKeyMap.get(key).get(mainTable.getString(key)));
                }else{
                    datum.add(mainTable.getString(key));
                }
            }
            failDataList.add(datum);
        }
        createMode(exportType,newFile,mapLabelNameKey,failDataList);
        fileName = newFile.getName();
        return fileName;
    }

    /**
     * 翻转map
     * @param originalMap 原map
     * @return key与value互换位置
     */
    public Map<String,Map<String, String>> swappedSelectMap(Map<String,Map<String, Integer>> originalMap){
        Map<String,Map<String, String>> swappedSelectMap = new HashMap<>();
        for(Map.Entry<String, Map<String, Integer>> entry:originalMap.entrySet()){
            swappedSelectMap.put(entry.getKey(),swappedMap(entry.getValue()));
        }
        return swappedSelectMap;
    }

    /**
     * 翻转map
     * @param originalMap 原map
     * @return key与value互换位置
     */
    public Map<String, String> swappedMap(Map<String, Integer> originalMap){
        Map<String, String> swappedMap = new HashMap<>();
        for (Map.Entry<String, Integer> entry : originalMap.entrySet()) {
            swappedMap.put(String.valueOf(entry.getValue()), entry.getKey());
        }
        return swappedMap;
    }

    /**
     *  单文件上传
     * @param networkSharePath   nas盘  地址
     * @param inputStream 文件输入流
     * @param fileName 附件名
     */
    public void saveFileToNAS(String networkSharePath,InputStream inputStream,String fileName) {
        try {
            if(networkSharePath.isEmpty()){
                throw new RuntimeException("nas地址为空！");
            }
            File workSharePath  = new File(networkSharePath);
            if(workSharePath.exists()) {
                File newFile = initNewFile(networkSharePath,fileName);
                Files.copy(inputStream,newFile.toPath());
                System.out.println("Attachment file successfully written to network share directory.");
            } else {
                System.out.println("Network share does not exist.");
            }
            inputStream.close();
        }catch(IOException e){
            throw new RuntimeException(e);
        }
    }
    
    /**
     * 导入档案附件
     * @param input 文件输入流
     * @param archiveFilePath 档案存放目录
     */
    public void exportArchiveFile(InputStream input,File archiveFilePath,String archiveType,String tableName,Map<String,String> relationMap){
        try (
                ZipInputStream zipInput = new ZipInputStream(input, Charset.forName("gbk"));
        ) {
            ZipEntry entry;
            Connection connection = JDBCUtils.getConnection();
            // 创建年月文件夹
            Calendar date = Calendar.getInstance();
            File dateDirs = new File(date.get(Calendar.YEAR) + File.separator + (date.get(Calendar.MONTH)+1));
            ExcelUtils excelUtils = new ExcelUtils();
            String newFilePath = File.separator + excelUtils.getExportFilePath() + File.separator+dateDirs;
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmssSS");
            while ((entry = zipInput.getNextEntry()) != null) {
                String entryFileName = entry.getName();
                String DH = entryFileName.contains(" ") ? entryFileName.substring(0,entryFileName.indexOf(" ")):entryFileName.substring(0,entryFileName.indexOf("."));//档号
                if(relationMap.containsKey(DH)){
                    String newFileName = sdf.format(new Date()) + "_" + entryFileName;
                    File newFile = initNewFile(archiveFilePath.toString(),entryFileName);
                    //输出流定义在try（）块，结束自动清空缓冲区并关闭
                    BufferedOutputStream bos=new BufferedOutputStream(new FileOutputStream(newFile));
                    //获取该子文件字节内容
                    byte[] buff = new byte[2048];
                    int len = -1;
                    while ((len = zipInput.read(buff)) != -1) {
                        bos.write(buff, 0, len);
                    }
                    bos.flush();
                    bos.close();
                    //建立档案与附件的联系
                    JDBCUtils jdbcUtils = new JDBCUtils();
                    jdbcUtils.establishRelArchiveFile(archiveType,tableName,DH,newFilePath,entryFileName,newFileName, getFileSize(entry.getSize()),relationMap);
                }
            }
            JDBCUtils.closeAll(null,null,connection);
            zipInput.closeEntry();
            input.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 导入档案附件
     * @param input 文件输入流
     * @param archiveFilePath 档案存放目录
     */
    public void exportArchiveFile(InputStream input,File archiveFilePath,String archiveType,String tableName){
        try (
                ZipInputStream zipInput = new ZipInputStream(input, Charset.forName("gbk"));
        ) {
            ZipEntry entry;
            Connection connection = JDBCUtils.getConnection();
            // 创建年月文件夹
            Calendar date = Calendar.getInstance();
            File dateDirs = new File(date.get(Calendar.YEAR) + File.separator + (date.get(Calendar.MONTH)+1));
            ExcelUtils excelUtils = new ExcelUtils();
            String newFilePath = File.separator + excelUtils.getExportFilePath() + File.separator+dateDirs;
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmssSS");
            while ((entry = zipInput.getNextEntry()) != null) {
                String entryFileName = entry.getName();
                String DH = entryFileName.contains(" ") ? entryFileName.substring(0,entryFileName.indexOf(" ")):entryFileName.substring(0,entryFileName.indexOf("."));//档号
                String newFileName = sdf.format(new Date()) + "_" + entryFileName;
                File newFile = initNewFile(archiveFilePath.toString(),entryFileName);
                //输出流定义在try（）块，结束自动清空缓冲区并关闭
                BufferedOutputStream bos=new BufferedOutputStream(new FileOutputStream(newFile));
                //获取该子文件字节内容
                byte[] buff = new byte[2048];
                int len = -1;
                while ((len = zipInput.read(buff)) != -1) {
                    bos.write(buff, 0, len);
                }
                bos.flush();
                bos.close();
                //建立档案与附件的联系
                JDBCUtils jdbcUtils = new JDBCUtils();
                jdbcUtils.establishRelArchiveFile(connection,archiveType,tableName,DH,newFilePath,entryFileName,newFileName, getFileSize(entry.getSize()));
            }
            JDBCUtils.closeAll(null,null,connection);
            zipInput.closeEntry();
            input.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getFileSize(long entrySize){
        //附件大小解析KB/MB
        String sizeUnit = "KB";
        double size = BigDecimal.valueOf((double) entrySize / 1024).setScale(2, RoundingMode.DOWN).doubleValue();
        if(size >= 1024){
            size = BigDecimal.valueOf((double) entrySize / 1024/1024).setScale(2, RoundingMode.DOWN).doubleValue();
            sizeUnit = "MB";
        }
        return size+sizeUnit;
    }

    public File initNewFile(String filePath, String fileName) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmssSS");
        String res = sdf.format(new Date());
        // 创建年月文件夹
        Calendar date = Calendar.getInstance();
        File dateDirs = new File(date.get(Calendar.YEAR) + File.separator + (date.get(Calendar.MONTH)+1));
        String newFilePath = filePath + File.separator+dateDirs;
        String newFileName = res + "_" + fileName;
        // 新文件
        File newFile = new File(newFilePath + File.separator+ newFileName);
        // 判断目标文件所在目录是否存在
        if( !newFile.getParentFile().exists()) {
            // 如果目标文件所在的目录不存在，则创建父目录
            newFile.getParentFile().mkdirs();
        }
        return newFile;
    }

    public static void main(String[] args) {
        //System.out.println(new ExcelUtils().getExportConf("ceshi1").get("modeFilePath").get(0));
        //new ExcelUtils().createMode("ceshi1");
        //new ExcelUtils().createMode("ceshi3");
        /*ExcelUtils excelUtils = new ExcelUtils();
        for(String str : excelUtils.getExportTypeList()){
            excelUtils.createMode(str);
        }*/
//        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmssSS");
//        String res = sdf.format(new Date());
//        String shareDir = "smb://172.21.21.40/数科/filesystem/2024/5/";
//        String localDir = "D://files/基建档案模板.xlsx";
//        String USER_DOMAIN = "172.21.21.40";
//        String USER_ACCOUNT = "sunriver";
//        String USER_PWS = "90op()OP";
//        smbPut(shareDir,localDir,USER_DOMAIN,USER_ACCOUNT,USER_PWS);
//        ExcelUtils excelUtils = new ExcelUtils();
//        try {
//            excelUtils.saveFileToNAS("\\\\172.21.21.40\\数科\\filesystem",new FileInputStream(new File("D://files/基建档案模板.xlsx")),"ceshi.xlsx");
//        } catch (FileNotFoundException e) {
//            throw new RuntimeException(e);
//        }

        long daysSinceEpoch = 45422;
        LocalDate epochDate = LocalDate.of(1899, 12, 30);
        LocalDate targetDate = epochDate.plusDays(daysSinceEpoch);

        System.out.println("targetDate="+targetDate);


        String dateString = "2024-05-10";
        LocalDate dateCur = LocalDate.parse(dateString);
        long daysSinceEpoch1 = ChronoUnit.DAYS.between(epochDate, dateCur);
        System.out.println("daysSinceEpoch1="+daysSinceEpoch1);

    }
}