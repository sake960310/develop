package com.sunriver.archiveSystem.util;

import java.io.IOException;
import java.sql.*;
import java.util.*;

import org.apache.commons.dbcp2.BasicDataSource;

public class JDBCUtils {
    public static void main(String[] args) {
        Connection conn = JDBCUtils.getConnection();
        System.out .println(conn);
    }
    private static final String PROPERTIES_FILE_PATH = "database.properties";
    private static final BasicDataSource dataSource = new BasicDataSource();
    private static final String jdbcUrl;
    private static final String username;
    private static final String password;
    private static final String driverClassName ;
    static {
        Properties prop = new Properties();
        try {
            prop.load(JDBCUtils.class.getClassLoader().getResourceAsStream(PROPERTIES_FILE_PATH));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        jdbcUrl = prop.getProperty("ecology.url");
        username = prop.getProperty("ecology.username");
        password = prop.getProperty("ecology.password");
        driverClassName = prop.getProperty("ecology.driverClassName");
        dataSource.setDriverClassName(driverClassName);
        dataSource.setUrl(jdbcUrl);
        dataSource.setUsername(username);
        dataSource.setPassword(password);

        // 设置连接池参数
        dataSource.setInitialSize(30); // 初始连接数
        dataSource.setMaxTotal(100); // 最大连接数
        dataSource.setMaxIdle(5); // 最大空闲连接数
        dataSource.setMinIdle(2); // 最小空闲连接数
    }

    public static Connection getConnection(){
        try {
            return dataSource.getConnection();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     *  获取档案查看参数
     * @param exportType 导入类型
     * @return 参数模块ID，表单ID
     */
    public Map<String,String> getArchiveViewData(Connection conn,String exportType){
        Map<String,String> map = new HashMap<>();
        //1.获取浏览框的数据表,获取显示值
        String getInfoSql = " SELECT ID MID,FORMID FID FROM MODEINFO WHERE MODENAME=? ";
        try {
            PreparedStatement statement = conn.prepareStatement(getInfoSql);
            statement.setString(1, exportType);//导入类型
            ResultSet resultSet = statement.executeQuery();
            while (resultSet.next()) {
                map.put("mid",resultSet.getString("mid"));
                map.put("fid",resultSet.getString("fid"));
            }
            closeAll(resultSet,statement,null);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return map;
    }

    /**
     * 动态生成插入sql语句
     * @param tableName  数据库表
     * @param columnNames  字段名集合
     * @return sql语句
     */
    public String generateInsertSql(String tableName, List<String> columnNames) {
        StringBuilder sqlBuilder = new StringBuilder("INSERT INTO ");
        sqlBuilder.append(tableName);
        sqlBuilder.append(" (");

        for (int i = 0; i < columnNames.size(); i++) {
            sqlBuilder.append(columnNames.get(i));
            if (i < columnNames.size() - 1) {
                sqlBuilder.append(", ");
            }
        }
        sqlBuilder.append(") VALUES (");
        for (int i = 0; i < columnNames.size(); i++) {
            sqlBuilder.append("?");
            if (i < columnNames.size() - 1) {
                sqlBuilder.append(", ");
            }
        }
        sqlBuilder.append(")");
        return sqlBuilder.toString();
    }

    /**
     *     获取浏览框实际值
     * @param cruValue  当前值
     * @param showname  浏览框显示值
     * @return 实际值
     */
    public String getBrowserActValue(String cruValue, String showname) {
        Connection conn = getConnection();
        String id="";
        //1.获取浏览框的数据表,获取显示值
        String getInfoSql = " select LISTAGG(fieldname,'') name,LISTAGG(sqltext,'') sqltext from (" +
                "select fieldname,'' sqltext from workflow_billfield where id=(select fieldid from mode_custombrowserdspfield where customid=(" +
                "select customid from mode_browser where showname=?) and istitle='1') union  select '' fieldname,sqltext from mode_browser " +
                "where showname=?) temp ";
        try {
            PreparedStatement statement = conn.prepareStatement(getInfoSql);
            statement.setString(1, showname);
            statement.setString(2, showname);
            ResultSet resultSet = statement.executeQuery();
            String browser_table_name="";
            String fieldname="";
            while (resultSet.next()) {
                fieldname = resultSet.getString("name");
                String sqltext = resultSet.getString("sqltext");
                browser_table_name = sqltext.substring(sqltext.indexOf("from ")+5, sqltext.indexOf("where")>0?sqltext.indexOf("where")-1:sqltext.length());
            }

            //2.根据指定表查出数据id
            String getValueIdSql = " select id from "+browser_table_name+" where "+fieldname+" =? ";
            statement = conn.prepareStatement(getValueIdSql);
            statement.setString(1,cruValue);
            resultSet = statement.executeQuery();
            while (resultSet.next()) {
                id = resultSet.getString("id");
            }
            closeAll(resultSet,statement,null);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return id;
    }

    /** 获取选择框实际值
     * @param tablename  表名
     * @param fieldname  字段名
     * @return 实际值
     */
    public Map<String,Integer> getSelectActValue(Connection conn,String tablename, String fieldname) {
        int selectvalue=0;
        //1.获取浏览框的数据表,获取显示值
        String getInfoSql = " select selectname,selectvalue from workflow_SelectItem where  fieldid=(select id from workflow_billfield where billid =(select id from workflow_bill where tablename=?) " +
                "and fieldname=?) ";
        Map<String,Integer> dataMap = new HashMap<>();
        try {
            PreparedStatement statement = conn.prepareStatement(getInfoSql);
            statement.setString(1, tablename);//表名
            statement.setString(2, fieldname);//字段名
            ResultSet resultSet = statement.executeQuery();
            while (resultSet.next()) {
                dataMap.put(resultSet.getString("selectname"),resultSet.getInt("selectvalue"));
            }
            closeAll(resultSet,statement,null);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return dataMap;
    }

    /**
     * 建立档案与附件的联系
     */
    public void establishRelArchiveFile(Connection conn,String archiveType,String tableName,String DH,String filePath,String fileName,String actFileName,String fileSize){
        //1.查询档案ID
        String sql = " select id from "+tableName+" where  DH=? ";
        String archiveId = "";
        try {
            PreparedStatement statement = conn.prepareStatement(sql);
            statement.setString(1, DH);//表名
            ResultSet resultSet = statement.executeQuery();
            while (resultSet.next()) {
                archiveId = resultSet.getString("id");
            }
            closeAll(resultSet,statement,null);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        //获取共享盘地址

        //封装附件数据
        Map<String,String> data = new HashMap<>();
        data.put("dh",DH);//档号
        data.put("fileName",fileName.substring(0,fileName.lastIndexOf(".")));//文件名
        data.put("fileType",fileName.substring(fileName.lastIndexOf(".")+1));//文件类型
        data.put("fileSize",fileSize);//文件大小
        data.put("filePath",filePath);//文件地址
        data.put("actFileName",actFileName);//实际存储名称
        data.put("archiveId",archiveId);//档案ID
        data.put("archiveType",archiveType);//档案类型
        data.put("archiveTable",tableName);//档案数据表

        //2.插入档案数据
        EcologyUtils ecologyUtils = new EcologyUtils();
        ecologyUtils.exportFileAction(data,"archivefile");
    }

    /**
     * 建立档案与附件的联系
     */
    public void establishRelArchiveFile(String archiveType,String tableName,String DH,String filePath,String fileName,String actFileName,String fileSize,Map<String,String> relationMap){
        //封装附件数据
        Map<String,String> data = new HashMap<>();
        data.put("dh",DH);//档号
        data.put("fileName",fileName.substring(0,fileName.lastIndexOf(".")));//文件名
        data.put("fileType",fileName.substring(fileName.lastIndexOf(".")+1));//文件类型
        data.put("fileSize",fileSize);//文件大小
        data.put("filePath",filePath);//文件地址
        data.put("actFileName",actFileName);//实际存储名称
        data.put("archiveId",relationMap.get(DH));//档案ID
        data.put("archiveType",archiveType);//档案类型
        data.put("archiveTable",tableName);//档案数据表
        //2.插入档案数据
        EcologyUtils ecologyUtils = new EcologyUtils();
        ecologyUtils.exportFileAction(data,"archivefile");
    }

    /**
     * 关闭数据库资源
     * @param resultSet 结果集
     * @param statement 查询清单
     * @param connection 链接
     */
    public static void closeAll(ResultSet resultSet, Statement statement, Connection connection){
        try {
            if (resultSet !=null && !resultSet.isClosed()) resultSet.close();
            if (statement !=null && !statement.isClosed()) statement.close();
            if (connection !=null && !connection.isClosed()) connection.close();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     *  获取字段-字段显示名   键值对
     * @param tablename 表名
     * @param fieldnameList  字段名集
     */
    public Map<String,String> getFieldLabelName(String tablename, List<String> fieldnameList){
        Connection conn = getConnection();
        String sql = " select  distinct m.INDEXID indexid,m.LABELNAME labelname,n.fieldname fieldname from HtmlLabelInfo m left join workflow_billfield n on m.INDEXID=n.fieldlabel where m.INDEXID in " +
                "(select fieldlabel from workflow_billfield where billid=(select id from workflow_bill where tablename=? ) and fieldname in (%?) ) ";
        Map<String,String> map = new HashMap<>();
        String temp = "?";
        for (int i = 1; i < fieldnameList.size(); i++) {
            temp+=",?";
        }
        sql = sql.replace("%?",temp);
        try {
            PreparedStatement statement = conn.prepareStatement(sql);
            statement.setString(1, tablename);//表名
            int index = 2;
            Iterator<String> fieldnameIterator = fieldnameList.iterator();
            while(fieldnameIterator.hasNext()){
                statement.setString(index, fieldnameIterator.next().replace("$1",""));//表名
                index++;
            }
            ResultSet resultSet = statement.executeQuery();
            while (resultSet.next()) {
                map.put(resultSet.getString("fieldname"),resultSet.getString("labelname"));
            }
            closeAll(resultSet,statement,conn);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return map;
    }

    /**
     *  获取字段-数据库名   键值对
     * @param tablename 表名
     * @param LabelNameList  字段显示名集
     */
    public Map<String,String> getFieldName(String tablename, List<String> LabelNameList){
        Connection conn = getConnection();
        String sql = " select  distinct m.INDEXID indexid,m.LABELNAME labelname,n.fieldname fieldname from HtmlLabelInfo m left join workflow_billfield n on m.INDEXID=n.fieldlabel where m.INDEXID in " +
                "(select fieldlabel from workflow_billfield where billid=(select id from workflow_bill where tablename=? ) and m.LABELNAME in (%?) ) ";
        Map<String,String> map = new HashMap<>();
        String temp = "?";
        for (int i = 1; i < LabelNameList.size(); i++) {
            temp+=",?";
        }
        sql = sql.replace("%?",temp);
        try {
            PreparedStatement statement = conn.prepareStatement(sql);
            statement.setString(1, tablename);//表名
            int index = 2;
            Iterator<String> fieldnameIterator = LabelNameList.iterator();
            while(fieldnameIterator.hasNext()){
                statement.setString(index, fieldnameIterator.next().replace("$1",""));//表名
                index++;
            }
            ResultSet resultSet = statement.executeQuery();
            while (resultSet.next()) {
                map.put(resultSet.getString("labelname"),resultSet.getString("fieldname"));
            }
            closeAll(resultSet,statement,conn);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return map;
    }
}