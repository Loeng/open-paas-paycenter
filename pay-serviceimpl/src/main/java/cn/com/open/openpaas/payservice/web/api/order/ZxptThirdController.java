package cn.com.open.openpaas.payservice.web.api.order;

import java.net.URLDecoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONObject;
import org.json.XML;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import com.alibaba.fastjson.JSON;

import cn.com.open.openpaas.payservice.app.channel.alipay.Channel;
import cn.com.open.openpaas.payservice.app.channel.alipay.PaymentType;
import cn.com.open.openpaas.payservice.app.channel.model.DictTradeChannel;
import cn.com.open.openpaas.payservice.app.channel.service.DictTradeChannelService;
import cn.com.open.openpaas.payservice.app.channel.zxpt.http.HttpClientUtil;
import cn.com.open.openpaas.payservice.app.channel.zxpt.rsa.RSACoderUtil;
import cn.com.open.openpaas.payservice.app.channel.zxpt.sign.MD5;
import cn.com.open.openpaas.payservice.app.channel.zxpt.xml.XOUtil;
import cn.com.open.openpaas.payservice.app.channel.zxpt.zx.DateUtil;
import cn.com.open.openpaas.payservice.app.channel.zxpt.zx.Request;
import cn.com.open.openpaas.payservice.app.channel.zxpt.zx.RequestHead;
import cn.com.open.openpaas.payservice.app.channel.zxpt.zx.Response;
import cn.com.open.openpaas.payservice.app.channel.zxpt.zx.ResponseHead;
import cn.com.open.openpaas.payservice.app.channel.zxpt.zx.ThirdScoreRequest;
import cn.com.open.openpaas.payservice.app.channel.zxpt.zx.ThirdScoreResponse;
import cn.com.open.openpaas.payservice.app.log.UnifyPayControllerLog;
import cn.com.open.openpaas.payservice.app.log.model.PayLogName;
import cn.com.open.openpaas.payservice.app.log.model.PayServiceLog;
import cn.com.open.openpaas.payservice.app.merchant.model.MerchantInfo;
import cn.com.open.openpaas.payservice.app.merchant.service.MerchantInfoService;
import cn.com.open.openpaas.payservice.app.order.model.MerchantOrderInfo;
import cn.com.open.openpaas.payservice.app.tools.BaseControllerUtil;
import cn.com.open.openpaas.payservice.app.tools.DateTools;
import cn.com.open.openpaas.payservice.app.tools.SysUtil;
import cn.com.open.openpaas.payservice.app.zxpt.model.PayZxptInfo;
import cn.com.open.openpaas.payservice.app.zxpt.service.PayZxptInfoService;
import cn.com.open.openpaas.payservice.dev.PayserviceDev;
import cn.com.open.openpaas.payservice.web.api.oauth.OauthSignatureValidateHandler;

/**
 *征信平台第三方
 */
@Controller
@RequestMapping("/zxpt/thirdScore")
public class ZxptThirdController extends BaseControllerUtil{
	private static final Logger log = LoggerFactory.getLogger(ZxptThirdController.class);
	
	 @Autowired
	 private PayZxptInfoService payZxptInfoService;
	 @Autowired
	 private MerchantInfoService merchantInfoService;
	 @Autowired
	 private DictTradeChannelService dictTradeChannelService;
	 @Autowired
	 private PayserviceDev payserviceDev;
	 


	/**
	 * 1006个人第三方评分请求
	 * @param request
	 * @param response
	 * @param model
	 * @return
	 * @throws Exception
	 */
    @RequestMapping("request")
    public String thirdScoreRequest(HttpServletRequest request,HttpServletResponse response,Model model) throws Exception  {
        long startTime = System.currentTimeMillis();
    	String fullUri=payserviceDev.getServer_host()+"zxpt/thirdScore/errorPayChannel";
        String appId = request.getParameter("appId");
    	String outTradeNo=request.getParameter("outTradeNo");
    	String certNo=request.getParameter("certNo");
    	String cardNo=request.getParameter("cardNo");
        String merchantId=request.getParameter("merchantId");
        String mobile=request.getParameter("mobile");
        String reasonNo=request.getParameter("reasonNo");
	    String channelId=request.getParameter("channelId");
        String signature=request.getParameter("signature");
	    String timestamp=request.getParameter("timestamp");
	    String signatureNonce=request.getParameter("signatureNonce");
	    String userName="";
    	if (!nullEmptyBlankJudge(request.getParameter("userName"))){
    		userName=new String(request.getParameter("userName").getBytes("iso-8859-1"),"utf-8");
    	}
    	String parameter="";
    	if (!nullEmptyBlankJudge(request.getParameter("parameter"))){
    		parameter=new String(request.getParameter("parameter").getBytes("iso-8859-1"),"utf-8");
    	}
		String newId="";
		newId=SysUtil.careatePayOrderId();
	    PayServiceLog payServiceLog=new PayServiceLog();
	    payServiceLog.setOrderId(newId);
	    payServiceLog.setAppId(appId);
	    payServiceLog.setChannelId("THIRD_SCORE");//
	    payServiceLog.setPaymentId("THIRD_SCORE");
	    payServiceLog.setCreatTime(DateTools.dateToString(new Date(), "yyyyMMddHHmmss"));
	    payServiceLog.setLogType(payserviceDev.getLog_type());
	    payServiceLog.setMerchantId(merchantId);
	    payServiceLog.setMerchantOrderId(outTradeNo);
    	payServiceLog.setStatus("ok");
    	payServiceLog.setLogName(PayLogName.THIRD_REQUEST_START);
    	UnifyPayControllerLog.log(startTime,payServiceLog,payserviceDev);
        if(!paraMandatoryCheck(Arrays.asList(outTradeNo,merchantId,appId,mobile,reasonNo,cardNo,certNo,userName,channelId))){
        	//paraMandaChkAndReturn(1, response,"必传参数中有空值");
        	payServiceLog.setErrorCode("1");
        	payServiceLog.setStatus("error");
        	payServiceLog.setLogName(PayLogName.THIRD_REQUEST_END);
        	UnifyPayControllerLog.log(startTime,payServiceLog,payserviceDev);
        	
        	return "redirect:" + fullUri+"?outTradeNo="+outTradeNo+"&errorCode="+"1";
        }
        //获取商户信息
    	MerchantInfo merchantInfo=merchantInfoService.findById(Integer.parseInt(merchantId));
    	if(merchantInfo==null){
        	//paraMandaChkAndReturn(2, response,"商户ID不存在");
    		payServiceLog.setErrorCode("2");
    		payServiceLog.setStatus("error");
    		payServiceLog.setLogName(PayLogName.THIRD_REQUEST_END);
    		UnifyPayControllerLog.log(startTime,payServiceLog,payserviceDev);
        	return "redirect:" + fullUri+"?outTradeNo="+outTradeNo+"&errorCode="+"2";
        }
    	SortedMap<Object,Object> sParaTemp = new TreeMap<Object,Object>();
    	sParaTemp.put("appId",appId);
   		sParaTemp.put("timestamp", timestamp);
   		sParaTemp.put("signatureNonce", signatureNonce);
   		sParaTemp.put("outTradeNo",outTradeNo );
   		sParaTemp.put("merchantId", merchantId);
   		sParaTemp.put("userName", userName);
   		sParaTemp.put("mobile", mobile);
   		sParaTemp.put("channelId", channelId);
   		sParaTemp.put("cardNo", cardNo);
   		sParaTemp.put("certNo", certNo);
   		sParaTemp.put("reasonNo", reasonNo);
   		sParaTemp.put("parameter", parameter);
   		String params=createSign(sParaTemp);
   	    Boolean hmacSHA1Verification=OauthSignatureValidateHandler.validateSignature(signature,params,merchantInfo.getPayKey());
		if(!hmacSHA1Verification){
			//paraMandaChkAndReturn(3, response,"认证失败");
			payServiceLog.setErrorCode("3");
			payServiceLog.setStatus("error");
			payServiceLog.setLogName(PayLogName.THIRD_REQUEST_END);
			UnifyPayControllerLog.log(startTime,payServiceLog,payserviceDev);
        	return "redirect:" + fullUri+"?outTradeNo="+outTradeNo+"&errorCode="+"3";
		}
		//查询第三方征信报告订单是否存在
		PayZxptInfo payZxptInfo=payZxptInfoService.findByMerchantOrderId(outTradeNo,appId);
		Boolean f=false;
		if(payZxptInfo!=null){
				payServiceLog.setErrorCode("5");
				payServiceLog.setStatus("error");
				payServiceLog.setLogName(PayLogName.THIRD_REQUEST_END);
				UnifyPayControllerLog.log(startTime,payServiceLog,payserviceDev);
	        	return "redirect:" + fullUri+"?outTradeNo="+outTradeNo+"&errorCode="+"5";
		}else{ 
			//创建订单
			payZxptInfo=new PayZxptInfo();
			payZxptInfo.setId(newId);
			payZxptInfo.setAppId(appId);
			payZxptInfo.setAuthDate(new Date());
			payZxptInfo.setCardNo(cardNo);
			payZxptInfo.setCertNo(certNo);
			payZxptInfo.setCertType(payserviceDev.getZxpt_cert_type());
			payZxptInfo.setEntityAuthCode(payserviceDev.getZxpt_entityAuthCode());
			payZxptInfo.setMerchantOrderId(outTradeNo);
			payZxptInfo.setPhone(mobile);
			payZxptInfo.setReasonNo(reasonNo);
			payZxptInfo.setScoreChannel(channelId);
			payZxptInfo.setScoreMethod(payserviceDev.getZxpt_score_method());
			payZxptInfo.setUserName(userName);
			f=payZxptInfoService.savePayZxptInfo(payZxptInfo);
			
		}
		if(f){
			//第三方评分交易码1006
			String paket = "";
			String sign = "";
			Request<ThirdScoreRequest> zxptrequest = new Request<ThirdScoreRequest>();
			RequestHead head = initHead("1006",DateUtil.getDateTime(new Date()));
			zxptrequest.setHead(head);
			List<ThirdScoreRequest> list = new ArrayList<ThirdScoreRequest>();
			ThirdScoreRequest rquestdetail = initThirdScoreRequest(payZxptInfo);
			list.add(rquestdetail);
			zxptrequest.setBody(list);
			String requestXml = XOUtil.objectToXml(zxptrequest, Request.class, RequestHead.class, ThirdScoreRequest.class);
//			XStream xstream=XMLUtil.fromXML(requestXml);
			System.out.println(requestXml);
			paket = RSACoderUtil.encrypt(requestXml.getBytes(payserviceDev.getZxpt_charset()), payserviceDev.getZxpt_charset(), payserviceDev.getZxpt_public_key());
			sign = MD5.sign(paket, "123456", "utf-8");
			
			//http请求参数
			Map<String, String> zxptParams = new HashMap<String, String>();
			//参数加密串
			zxptParams.put("packet", paket);
			//MD5签名
			zxptParams.put("checkValue", sign);
			//交易码
			zxptParams.put("tranCode", "1006");
			//商户号 奥鹏
			zxptParams.put("sender", payserviceDev.getZxpt_sender());
			//SIT环境
			String url = payserviceDev.getZxpt_third_url();
			//http请求
			String responsexml = HttpClientUtil.httpPost(url, zxptParams);
			//解密
			String decode = new String(RSACoderUtil.decrypt(responsexml, payserviceDev.getZxpt_key(), payserviceDev.getZxpt_charset()), payserviceDev.getZxpt_charset());
			String decodexml = URLDecoder.decode(decode, payserviceDev.getZxpt_charset());
			
			JSONObject xmlJSONObj = XML.toJSONObject(decodexml);
    		com.alibaba.fastjson.JSONObject jsonObject = null;
    		jsonObject = JSON.parseObject(xmlJSONObj.toString());
    		String zxpt=jsonObject.getString("zxpt");
    		com.alibaba.fastjson.JSONObject zxptjson=JSON.parseObject(zxpt);
    		String body=zxptjson.getString("body");
    		com.alibaba.fastjson.JSONObject bodyjson=JSON.parseObject(body);
    		String bqsfraudlistresponse=bodyjson.getString("bqsfraudlistresponse");
    		com.alibaba.fastjson.JSONObject bqsresponsejson=JSON.parseObject(bqsfraudlistresponse);
    		String orderId=bqsresponsejson.getString("orderId");
    		String orderNo=bqsresponsejson.getString("orderNo");
    		String errorCode=bqsresponsejson.getString("errorCode");
			
			fullUri=payserviceDev.getServer_host()+"/zxpt/thirdScore/thirdScoreBack?decodexml="+decodexml;
    	    payServiceLog.setLogName(PayLogName.THIRD_REQUEST_END);
		    UnifyPayControllerLog.log(startTime,payServiceLog,payserviceDev);	 	   
		  }
		  return "redirect:" + fullUri;
    }
  
    /**
     * 返回绑卡成功参数
     */
    @RequestMapping(value = "thirdScoreBack", method = RequestMethod.GET)
    public void bindBack(HttpServletRequest request,HttpServletResponse response, Model model){
    	String decodexml=request.getParameter("decodexml");
    	
    	
    	
    	
    	
    	
    	
    	
    	
    	
    	
    	
    	
    	
    	
    	
    	
    	
    	
    	
    	Response<ThirdScoreResponse>packet=new Response<ThirdScoreResponse>();
    	 Map<String, Object> map=new HashMap<String,Object>();
    	try {
    		//将xml转成对象
			XOUtil.xmlToObject(decodexml,packet,payserviceDev.getZxpt_charset(),Response.class, ResponseHead.class, ThirdScoreResponse.class);
		    if(packet.getBody()==null||packet.getBody().size()==0){
	    	 map.put("status", "error");
	    	 map.put("errMsg", "获取body失败");
	    	 map.put("errorCode","T10001");
		    }else{
		    	//获取返回信息的body内容
		    	List<ThirdScoreResponse>list=packet.getBody();
		    	ThirdScoreResponse thirdScoreResponse=null;
		    	if(list!=null && list.size()>0){
		    		thirdScoreResponse=list.get(0);
		    	}
		    //业务方请求流水号
		    String orderId=thirdScoreResponse.getOrderId();
		    //第三方征信平台流水号
		    String orderNo=thirdScoreResponse.getOrderNo();
		    String credooScore="";
		    Map<String ,Object> reponsemap=thirdScoreResponse.getMap();
		    //获取明细数据
		    List<Map<String ,String>>recordsList=(List<Map<String, String>>) reponsemap.get("records");
		    if(recordsList!=null&&recordsList.size()>0){
		    	Map<String ,String> recordsMap=recordsList.get(0);
		    	credooScore=recordsMap.get("credooScore");
		    	map.clear();
		    	map.put("status", "ok");
		    	map.put("errMsg", "");
		    	map.put("credooScore",credooScore);
		    	map.put("errorCode", "");
		    	//更新订单征信平台流水号以及订单状态
		    	PayZxptInfo payZxptInfo= payZxptInfoService.findById(orderId);
		    	if(payZxptInfo!=null){
		    		payZxptInfo.setZxptOrderNo(orderNo);
		    		payZxptInfo.setZxptRequestStatus("1");
		    		payZxptInfoService.updateZxptInfo(payZxptInfo);
		    	}else{
		    		 map.clear();
			    	 map.put("status", "error");
			    	 map.put("errMsg", "订单信息查询失败");
			    	 map.put("errorCode", "N10002");	
		    	}
		    }else{
		    	 map.clear();
		    	 map.put("status", "error");
		    	 map.put("errMsg", "获取明细数据失败");
		    	 map.put("errorCode", "N10001");
		      }
		    }
    	} catch (Exception e) {
    		 map.clear();
	    	 map.put("status", "error");
	    	 map.put("errMsg", "获取明细数据失败");
	    	 map.put("errorCode", "N10001");
	    	 e.printStackTrace();
		}
    	 writeSuccessJson(response,map);
    }
    /**
     * 返回错误信息
     */
    @RequestMapping(value = "errorPayChannel", method = RequestMethod.GET)
    public void payError(HttpServletRequest request,HttpServletResponse response, Model model,String errorCode,String outTradeNo,String failureCode,String failureMsg){
    	 Map<String, Object> map=new HashMap<String,Object>();
    		String errorMsg="";
    		if(!nullEmptyBlankJudge(errorCode)&&errorCode.equals("1")){
        		errorMsg="必传参数中有空值";
        	}else if(!nullEmptyBlankJudge(errorCode)&&errorCode.equals("3")){
        		errorMsg="验证失败";
        	}else if(!nullEmptyBlankJudge(errorCode)&&errorCode.equals("5")){
        		errorMsg="所选支付渠道与支付类型不匹配";
        	}else if(!nullEmptyBlankJudge(errorCode)&&errorCode.equals("2")){
        		errorMsg="商户ID不存在";
        	}else if(!nullEmptyBlankJudge(errorCode)&&errorCode.equals("4")){
        		errorMsg="金额格式不对";
        	}else if(!nullEmptyBlankJudge(errorCode)&&errorCode.equals("5")){
        		errorMsg="订单号重复";
        	}else if(!nullEmptyBlankJudge(errorCode)&&errorCode.equals("6")){
        		errorMsg="用户未绑定卡号";
        	}else if(!nullEmptyBlankJudge(errorCode)&&errorCode.equals("7")){
        		errorMsg="无短充值失败！错误码:"+failureCode+"--错误原因："+failureMsg;
        	}else if(!nullEmptyBlankJudge(errorCode)&&errorCode.equals("8")){
        		errorMsg="无短充值验签失败";
        	}else if(!nullEmptyBlankJudge(errorCode)&&errorCode.equals("9")){
        		errorMsg="用户不存在";
        	}
        	else if(!nullEmptyBlankJudge(errorCode)&&errorCode.equals("10")){
        		errorMsg="渠道未开通！";
        	}
    	   map.put("status", "error");
    	   map.put("requestno", outTradeNo);
    	   map.put("errorCode", errorCode);
    	   map.put("errMsg", errorMsg);
    	   writeSuccessJson(response,map);
    	 // WebUtils.writeJson(response, urlCode);
    	   
    	   
    }
    public static RequestHead initHead(String tranCode,String tranId) {
		//请求消息头
		RequestHead rh = new RequestHead();
		SimpleDateFormat df = new SimpleDateFormat("yyyyMMddHHmmss");
		SimpleDateFormat df2 = new SimpleDateFormat("yyyyMMdd");
		//初始化报文头
		rh.setVersion("V1.0");
		rh.setCharSet("utf-8");
		rh.setSource("奥鹏教育学生平台");
		rh.setDes("zxpt");
		rh.setApp("奥鹏App");
		rh.setTranCode(tranCode);
		rh.setTranId(tranId);
		rh.setTranRef("奥鹏商户");
		rh.setReserve("奥鹏商户测试");
		rh.setTranTime(df2.format(new Date()));
		rh.setTimeStamp(df.format(new Date()));
		return rh;
	}
	
	public static ThirdScoreRequest initThirdScoreRequest(PayZxptInfo payZxptInfo) {
		
		ThirdScoreRequest thirdScoreRequest = new ThirdScoreRequest();
		thirdScoreRequest.setOrderId(DateUtil.getCurrentDateTime());
		
		thirdScoreRequest.setCertNo("420922198509103814");
		thirdScoreRequest.setCertType("0");
		thirdScoreRequest.setName("张四");
		thirdScoreRequest.setReserve("第三方评分测试");
		thirdScoreRequest.setScoreChannel("2");
		thirdScoreRequest.setScoreMethod("1");
		thirdScoreRequest.setMobile("13535413805");
		thirdScoreRequest.setCardNo("62222744552211111112");
		thirdScoreRequest.setReasonNo("01");
		thirdScoreRequest.setEntityAuthCode("02aa19bc");
		thirdScoreRequest.setAuthDate("2016-10-12 12:01:01");
		//请求消息体
	/*	ThirdScoreRequest thirdScoreRequest = new ThirdScoreRequest();
		thirdScoreRequest.setOrderId(payZxptInfo.getId());
		
		thirdScoreRequest.setCertNo(payZxptInfo.getCertNo());
		thirdScoreRequest.setCertType(payZxptInfo.getCertType());
		thirdScoreRequest.setName(payZxptInfo.getUserName());
		thirdScoreRequest.setReserve(payZxptInfo.getReserve());
		thirdScoreRequest.setScoreChannel(payZxptInfo.getScoreChannel());
		thirdScoreRequest.setScoreMethod(payZxptInfo.getScoreMethod());
		thirdScoreRequest.setMobile(payZxptInfo.getPhone());
		thirdScoreRequest.setCardNo(payZxptInfo.getCardNo());
		thirdScoreRequest.setReasonNo(payZxptInfo.getReasonNo());
		thirdScoreRequest.setEntityAuthCode(payZxptInfo.getEntityAuthCode());
		thirdScoreRequest.setAuthDate(DateTools.dateToString(payZxptInfo.getAuthDate(), DateTools.FORMAT_ONE));*/
		return thirdScoreRequest;
	}
	

}