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
        .btn[disabled] {
            background-color: #ccc;
            color: #666;
            cursor: not-allowed;
        }
        /* 弹出框样式 */
        .modal {
            display: none; /* 默认隐藏 */
            position: fixed;
            top: 30%;
            left: 50%;
            transform: translate(-50%, -50%);
            border: 1px solid #ccc;
            background-color: #fff;
            padding: 20px;
            box-shadow: 0 2px 4px rgba(0, 0, 0, 0.2);
            z-index: 1000;
            width: 300px;
            text-align: center;
        }

        .modal h2 {
            margin-top: 0;
        }

        .modal p {
            margin-bottom: 20px;
        }

        .modal button {
            padding: 10px 20px;
            margin: 0 10px;
            border: none;
            background-color: #007bff;
            color: #fff;
            cursor: pointer;
        }

        .modal button:hover {
            background-color: #0056b3;
        }
    </style>

    <script>

    </script>
</head>
<body>
<div class="container">
    <h1>文件导入界面</h1>
    <div class="form-group">
        <label>文件模版下载：</label>
        <form id="downloadForm" action="download" method="post" enctype="multipart/form-data" accept-charset="UTF-8">
            <input id="downloadType" name="downloadType" value=""  style="display: none;">
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
        <label>档案数据文件上传：</label>
        <input type="file" name="fileData" class="form-control" id="dataFile">
    </div>
    <div class="form-group">
        <label>档案附件zip上传：</label>
        <input type="file" name="archiveFile" class="form-control" id="zipFile">
    </div>
    <button class="btn" id="submitBtn" disabled>上传提交</button>
    <!-- 弹出框内容 -->
    <div id="myModal" class="modal">
        <h2>信息确认</h2>
        <p id="myModalContent"></p>
        <button id="confirmBtn">确定</button>
    </div>

    <div class="form-group" id="failData" style="display: none;">
        <label>点击下载失败文件：</label>
        <a id="failDataUrl" style="cursor: pointer;color: #007bff;" href="javascript:void(0)">下载失败文件</a>
    </div>
</div>
<script>
    $(function() {
        // 监听fileData文件输入框的变化
        $('#dataFile').change(function() {
            var file = $(this)[0].files[0]; // 获取选择的文件
            if (file) {
                var fileName = file.name;
                var fileExt = fileName.split('.').pop(); // 获取文件扩展名
                if (fileExt.toLowerCase() === 'xlsx') {
                    $('#submitBtn').removeAttr('disabled'); // 文件格式符合要求，移除disabled属性
                } else {
                    alert('请上传xlsx格式的文件'); // 文件格式不符合要求，弹出提示
                    $(this).val(''); // 清空fileData文件输入框的值
                    $('#submitBtn').attr('disabled', true); // 添加disabled属性，禁止点击按钮
                }
            } else {
                $('#submitBtn').attr('disabled', true); // 文件为空，添加disabled属性，禁止点击按钮
            }
        });

        $('#zipFile').change(function() {
            var file = $(this)[0].files[0]; // 获取选择的文件
            if (file) {
                var fileName = file.name;
                var fileExt = fileName.split('.').pop(); // 获取文件扩展名
                if (fileExt.toLowerCase() !== 'zip') {
                    alert('请上传zip格式的文件'); // 文件格式不符合要求，弹出提示
                    $(this).val(''); // 清空fileData文件输入框的值
                }
            }
        });
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
            $('#downloadForm').submit();
        });

        //文件上传触发请求
        $("#submitBtn").click(function() {
            $(this).prop("disabled", true).text("上传中..."); // 按钮进入等待状态
            var formData = new FormData();
            formData.append("fileData", $("#dataFile")[0].files[0]);
            formData.append("archiveFile", $("#zipFile")[0].files[0]);
            formData.append("exportType", $("#exportType").val());
            debugger;
            $.ajax({
                url: "/archiveSystem/upload",
                type: "POST",
                data: formData,
                processData: false,
                contentType: false,
                success: function(response) {
                    // 请求成功处理逻辑
                    $("#submitBtn").prop("disabled", false).text("上传提交"); // 恢复按钮状态
                    debugger;
                    var result = JSON.parse(response);
                    var content = "";
                    var failMode = result.failMode;
                    if(failMode === "1"){
                        content = "导入模版错误，请重新下载导入模版！";
                    }else{
                        var successData = result.successData;
                        var successBaseData = result.successBaseData;
                        var failData = result.failData;
                        var failBaseData = result.failBaseData;
                        var failDataName = result.failDataName;
                        content = "档案数据:成功插入：" + successData+" 条,失败 "+failData+"条;\r基表数据：成功插入："+ successBaseData+" 条,失败 "+failBaseData+"条。";
                        var failDataUrl = $("#failDataUrl");
                        failDataUrl.attr("href","download?fileName="+failDataName);
                        failDataUrl.text(failDataName);
                        if(failData > 0){
                            $("#failData").show();
                        }
                    }
                    $('#archiveFile').val('');
                    $('#fileData').val('');
                    $('#myModalContent').text(content);
                    $('#myModal').show();
                },
                error: function() {
                    // 请求失败处理逻辑
                    $("#submitBtn").prop("disabled", false).text("上传提交"); // 恢复按钮状态
                    alert("请求失败，请稍后重试");
                }
            });
        });
        // 点击确定按钮
        $("#confirmBtn").click(function() {
            $('#myModal').hide();
            $('#dataFile').val('');
            $('#zipFile').val('');
        })

        $("#failDataUrl").click(function() {
            $("#failData").hide();
        })
    });
</script>
</body>
</html>
