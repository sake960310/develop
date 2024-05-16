<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="com.sunriver.archiveSystem.util.ExcelUtils" %>
<%@ page import="java.util.*" %>
<script src="https://code.jquery.com/jquery-3.6.0.min.js"></script>
<%
    String path = request.getContextPath();
    String basePath = request.getScheme()+"://"+request.getServerName()+":"+request.getServerPort()+path+"/";
	ExcelUtils excelUtils = new ExcelUtils();
	Map<String,String> map = excelUtils.getExportTypeMap();
    request.setAttribute("map", map);
%>
<!DOCTYPE html>
<html>
<head>
    <base href="<%=basePath%>">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>File Upload</title>
    <style>
        body {
            font-family: Arial, sans-serif;
            background-color: #f8f9fa;
        }
        .container {
            max-width: 600px;
            margin: 50px auto;
            padding: 20px;
            background-color: #ffffff;
            border-radius: 5px;
            box-shadow: 0 0 10px rgba(0, 0, 0, 0.1);
        }
        h1 {
            color: #007bff;
            text-align: center;
        }
        .form-group {
            margin-bottom: 20px;
        }
        .form-group label {
            display: block;
            font-weight: bold;
        }
        .form-control {
            width: 100%;
            padding: 8px;
            font-size: 16px;
            border: 1px solid #ced4da;
            border-radius: 4px;
        }
        .btn {
            padding: 10px 20px;
            background-color: #007bff;
            color: #ffffff;
            border: none;
            border-radius: 4px;
            cursor: pointer;
        }
    </style>

    <script>

    </script>
</head>
<body>
<div class="container">
    <h1>历史附件关联关系导入</h1>
    <div class="form-group">
        <label>导入模版下载：</label>
        <form id="downloadForm" action="download" method="post" enctype="multipart/form-data" accept-charset="UTF-8">
            <input id="downloadType" name="downloadType" value=""  style="display: none;">
            <input name="isHistory" value="1"  style="display: none;">
            <label id="downloadTitle" name="downloadTitle" style="cursor: pointer;color: #007bff;">请选择导入类型</label>
        </form>
    </div>
    <div class="form-group">
        <label>选择导入类型：</label>
        <label>
            <select name="exportType" id="exportType">
                <option value="">请选择导入类型</option>
                <c:forEach items="${map}" var="entry">
                    <option value="${entry.key}">${entry.key}</option>
                </c:forEach>
            </select>
        </label>
    </div>
    <div class="form-group">
        <label>历史档案数据文件上传(.xlsx)：</label>
        <input type="file" name="fileData" class="form-control" id="dataFile">
    </div>
    <button class="btn" id="submitBtn">提交</button>
</div>
<script>
    $(document).ready(function() {
        //类型选择触发模版下载名称变更
        $('select[name="exportType"]').change(function() {
            var downloadTitle = $('#downloadTitle');
            var downloadType = $('#downloadType');
            if ($(this).val() === "") {
                downloadType.val('');
                downloadTitle.text('请选择导入类型');
            } else {
                downloadType.val(this.value+'_模版.xlsx');
                downloadTitle.text($(this).val() + '_模版.xlsx');
            }
        });

        //文件模版下载
        $("#downloadTitle").click(function() {
            var title = $(this).text();
            //$(this).prop("disabled", true).text("正在下载..."); // 按钮进入等待状态
            $('#downloadForm').submit();
            /*$.ajax({
                url: "http://localhost:8080/archiveSystem/download",
                type: "POST",
                dataType: 'json',
                data: { isHistory: '', downloadType: $('#downloadType').val() },
                success: function(response) {
                    // 请求成功处理逻辑
                    $("#submitBtn").prop("disabled", false).text("上传提交"); // 恢复按钮状态
                    alert("请求成功，接口返回值：" + response);
                },
                error: function() {
                    // 请求失败处理逻辑
                    $("#submitBtn").prop("disabled", false).text("上传提交"); // 恢复按钮状态
                    alert("请求失败，请稍后重试");
                }
            });*/
        });

        //文件上传触发请求
        $("#submitBtn").click(function() {
            $(this).prop("disabled", true).text("上传中..."); // 按钮进入等待状态

            var formData = new FormData();
            formData.append("fileData", $("#dataFile")[0].files[0]);
            formData.append("exportType", $("#exportType").val());

            $.ajax({
                url: "http://localhost:8080/archiveSystem/hisexport",
                type: "POST",
                data: formData,
                processData: false,
                contentType: false,
                success: function(response) {
                    // 请求成功处理逻辑
                    $("#submitBtn").prop("disabled", false).text("上传提交"); // 恢复按钮状态
                    alert("请求成功，接口返回值：" + response);
                },
                error: function() {
                    // 请求失败处理逻辑
                    $("#submitBtn").prop("disabled", false).text("上传提交"); // 恢复按钮状态
                    alert("请求失败，请稍后重试");
                }
            });
        });
    });
</script>
</body>
</html>
