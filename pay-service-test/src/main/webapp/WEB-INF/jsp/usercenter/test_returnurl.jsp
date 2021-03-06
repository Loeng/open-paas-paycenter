<%--
 * 
 * @author Shengzhao Li
--%>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<!DOCTYPE HTML>
<html>
<head>
    <title>user-order-query(interface)</title>
    <script src="${contextPath}/js/jquery-1.7.1.min.js"></script>
</head>

<body>
<a href="${contextPath}/">_Home</a>


<div class="panel panel-default">
    <div class="panel-body">
        <div ng-controller="AuthorizationCodeCtrl" class="col-md-10">

            <form action="http://10.96.5.164:86/StudentAreas/Pay/TongYiZhiFuPayReturn" method="post" class="form-horizontal">
            <%-- <form action="${userCenterRegUri}" method="post" class="form-horizontal"> --%>
                <input type="hidden" name="querySerialUri" id="querySerialUri" value="${querySerialUri}"/>
                <a href="javascript:void(0);" ng-click="showParams()">显示请求参数</a>

                <div ng-show="visible">
                   
                   
                   
                     <div class="form-group">
                        <label class="col-sm-2 control-label">goodsName</label>

                        <div class="col-sm-10">
                            <input type="text" name="goodsName" id="goodsName"
                                   class="form-control" ng-model="goodsName"/>

                            <p class="help-block">商品名称</p>
                        </div>
                    </div>
                    <div class="well well-sm">
                         <span class="text-muted">最终发给 pay-service-server的 URL:</span>
                        <br/>

                        <div class="text-primary" id="userOrderRefundUri"></div> 
                    </div>
                </div>
                <br/>
                <br/>
                <button type="submit" class="btn btn-primary">调用统一扣费接口</button>
                <button type="button"  class="btn btn-primary" onclick="btnSubmit();">获取接口调用地址</button>
            </form>

        </div>
    </div>
</div>
<script>
    var AuthorizationCodeCtrl = ['$scope', function ($scope) {
		$scope.appId="10026";
		$scope.merchantId="10001";
		$scope.startTime="2016-05-18 16:03:25";
		$scope.endTime="2016-07-28 16:03:25";
		$scope.payType="1";
		http://10.96.14.88:53446/pay/payment/PostPayNotice
        $scope.visible = false;
        $scope.showParams = function () {
            $scope.visible = !$scope.visible;
        };
    }];
</script>
<script type="text/javascript">
	function btnSubmit(){
		var appId=$("#appId").val();
		var startTime=$("startTime").val();
		var merchantId=$("merchantId").val();
		var endTime=$("endTime").val();
	    var querySerialRecord=$("#querySerialRecord").val();
	    var payType=$("#payType").val();
		if(startTime==''){
		    alert("请输入开始时间");
			return;
		}if(appId==''){
		    alert("请输入appId");
			return;
		}if(payType==''){
		    alert("请选择消费类型");
			return;
		}if(endTime==''){
		    alert("请输入结束时间");
			return;
		}
		if(merchantId==''){
		    alert("请输入商户号");
			return;
		}
		
		$.post("${contextPath}/getSignature",
			{
				appId:appId
			},
			function(data){
				if(data.flag){
				    var signature=data.signature;
				    var timestamp=data.timestamp;
				    var signatureNonce=data.signatureNonce;
				    var regUri=querySerialRecord+"?"+"merchantId="+merchantId+"pay_type="+payType+"&app_id="+appId+"&start_time="+startTime+"&end_time="+endTime+"&signature="+signature+"&amptimestamp="+timestamp+"&signatureNonce="+signatureNonce;
					$("#userOrderRefundUri").html(regUri);
				}else{
				    jQuery("#userOrderRefundUri").html('无效数据，请重新申请');
				}
			}
 		);
	}
</script>

</body>
</html>