package cn.com.open.openpaas.payservice.app.channel.yeepay;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Date;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.sf.json.JSONObject;

import org.dom4j.DocumentException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import cn.com.open.openpaas.payservice.app.balance.service.UserAccountBalanceService;
import cn.com.open.openpaas.payservice.app.channel.UnifyPayUtil;
import cn.com.open.openpaas.payservice.app.channel.alipay.AliOrderProThread;
import cn.com.open.openpaas.payservice.app.log.UnifyPayControllerLog;
import cn.com.open.openpaas.payservice.app.log.model.PayLogName;
import cn.com.open.openpaas.payservice.app.log.model.PayServiceLog;
import cn.com.open.openpaas.payservice.app.merchant.service.MerchantInfoService;
import cn.com.open.openpaas.payservice.app.order.model.MerchantOrderInfo;
import cn.com.open.openpaas.payservice.app.order.service.MerchantOrderInfoService;
import cn.com.open.openpaas.payservice.app.record.service.UserSerialRecordService;
import cn.com.open.openpaas.payservice.app.tools.BaseControllerUtil;
import cn.com.open.openpaas.payservice.app.tools.DateTools;
import cn.com.open.openpaas.payservice.app.tools.WebUtils;
import cn.com.open.openpaas.payservice.dev.PayserviceDev;


/**
 * 
 */
@Controller
@RequestMapping("/yeepay/notify/")
public class YeeNotifyCallbackController extends BaseControllerUtil {
	private static final Logger log = LoggerFactory.getLogger(YeeNotifyCallbackController.class);
	 @Autowired
	 private MerchantOrderInfoService merchantOrderInfoService;
	 @Autowired
	 private MerchantInfoService merchantInfoService;
	 @Autowired
	 private PayserviceDev payserviceDev;
	 @Autowired
	 private UserAccountBalanceService userAccountBalanceService;
	 @Autowired
	 private UserSerialRecordService userSerialRecordService;
	/**
	 * 支付宝订单回调接口
	 * @param request
	 * @param response
	 * @throws IOException 
	 * @throws DocumentException 
	 * @throws MalformedURLException 
	 */
	@RequestMapping("callBack")
	public void dirctPay(HttpServletRequest request,HttpServletResponse response,Map<String,Object> model) throws MalformedURLException, DocumentException, IOException {
		long startTime = System.currentTimeMillis();
		String r0_Cmd 	  = formatString(request.getParameter("r0_Cmd")); // 业务类型
		String p1_MerId   = formatString(Configuration.getInstance().getValue("p1_MerId"));   // 商户编号
		String r1_Code    = formatString(request.getParameter("r1_Code"));// 支付结果
		String r2_TrxId   = formatString(request.getParameter("r2_TrxId"));// 易宝支付交易流水号
		String r3_Amt     = formatString(request.getParameter("r3_Amt"));// 支付金额
		String r4_Cur     = formatString(request.getParameter("r4_Cur"));// 交易币种
		String r5_Pid     = new String(formatString(request.getParameter("r5_Pid")).getBytes("iso-8859-1"),"gbk");// 商品名称
		String r6_Order   = formatString(request.getParameter("r6_Order"));// 商户订单号
		String r7_Uid     = formatString(request.getParameter("r7_Uid"));// 易宝支付会员ID
		String r8_MP      = new String(formatString(request.getParameter("r8_MP")).getBytes("iso-8859-1"),"gbk");// 商户扩展信息
		String r9_BType   = formatString(request.getParameter("r9_BType"));// 交易结果返回类型
		String hmac       = formatString(request.getParameter("hmac"));// 签名数据
		String keyValue   = formatString(Configuration.getInstance().getValue("keyValue")); 
		MerchantOrderInfo merchantOrderInfo=merchantOrderInfoService.findById(r6_Order);
		String 	backMsg="";
		if(merchantOrderInfo!=null){
			Double total_fee=0.0;
			if(nullEmptyBlankJudge(r3_Amt)){
				total_fee=Double.parseDouble(r3_Amt);
			}
			
			boolean isOK = false;
			//添加日志
			 PayServiceLog payServiceLog=new PayServiceLog();
			 payServiceLog.setAmount(r3_Amt);
			 payServiceLog.setAppId(merchantOrderInfo.getAppId());
			 payServiceLog.setChannelId(String.valueOf(merchantOrderInfo.getChannelId()));
			 payServiceLog.setCreatTime(DateTools.dateToString(new Date(), "yyyy-MM-dd HH:mm:ss"));
			 payServiceLog.setLogType(payserviceDev.getLog_type());
			 payServiceLog.setMerchantId(merchantOrderInfo.getMerchantId()+"");
			 payServiceLog.setMerchantOrderId(merchantOrderInfo.getMerchantOrderId());
			 payServiceLog.setOrderId(r2_TrxId);
			 payServiceLog.setPaymentId(merchantOrderInfo.getPaymentId()+"");
			 payServiceLog.setPayOrderId(r2_TrxId);
			 payServiceLog.setProductName(r5_Pid);
			 payServiceLog.setRealAmount(r3_Amt);
			 payServiceLog.setSourceUid(merchantOrderInfo.getSourceUid());
			 payServiceLog.setUsername(merchantOrderInfo.getUserName());
			 payServiceLog.setStatus("ok");
			 payServiceLog.setLogName(PayLogName.CALLBACK_START);
			 UnifyPayControllerLog.log(startTime,payServiceLog,payserviceDev);
			// 校验返回数据包
			 
			isOK = PaymentForOnlineService.verifyCallback(hmac,p1_MerId,r0_Cmd,r1_Code, 
					r2_TrxId,r3_Amt,r4_Cur,r5_Pid,r6_Order,r7_Uid,r8_MP,r9_BType,keyValue);
			
			if(isOK) {
				//在接收到支付结果通知后，判断是否进行过业务逻辑处理，不要重复进行业务逻辑处理
				if(r1_Code.equals("1")) {
					// 产品通用接口支付成功返回-浏览器重定向
					if(r9_BType.equals("1")) {
						//out.println("callback方式:产品通用接口支付成功返回-浏览器重定向");
						// 产品通用接口支付成功返回-服务器点对点通讯
					} else if(r9_BType.equals("2")) {
						// 如果在发起交易请求时	设置使用应答机制时，必须应答以"success"开头的字符串，大小写不敏感
						
						backMsg="SUCCESS";
						
						//账户充值操作
						String rechargeMsg="";
						if(merchantOrderInfo!=null&&!nullEmptyBlankJudge(String.valueOf(merchantOrderInfo.getBusinessType()))&&"2".equals(String.valueOf(merchantOrderInfo.getBusinessType()))){
							
							rechargeMsg=UnifyPayUtil.recordAndBalance(total_fee,merchantOrderInfo,userSerialRecordService,userAccountBalanceService,payserviceDev);
							  
						}
						int notifyStatus=merchantOrderInfo.getNotifyStatus();
						int payStatus=merchantOrderInfo.getPayStatus();
						Double payCharge=0.0;
						
						if(payStatus!=1){
							merchantOrderInfo.setPayStatus(1);
							merchantOrderInfo.setPayAmount(total_fee-payCharge);
							merchantOrderInfo.setAmount(total_fee);
							merchantOrderInfo.setPayCharge(0.0);
							merchantOrderInfo.setDealDate(new Date());
							merchantOrderInfo.setPayOrderId(r2_TrxId);
							merchantOrderInfoService.updateOrder(merchantOrderInfo);
						}
						if(notifyStatus!=1){
							 Thread thread = new Thread(new AliOrderProThread(merchantOrderInfo, merchantOrderInfoService,merchantInfoService,rechargeMsg,payserviceDev));
							 thread.run();	
						}
						payServiceLog.setLogName(PayLogName.CALLBACK_END);
						UnifyPayControllerLog.log(startTime,payServiceLog,payserviceDev);
					}
				}
			  } else {
				payServiceLog.setLogName(PayLogName.CALLBACK_END);
				payServiceLog.setStatus("error");
				UnifyPayControllerLog.log(startTime,payServiceLog,payserviceDev);
				backMsg="error";
			}	
		}else{
			backMsg="error";
		}
		WebUtils.writeJson(response, backMsg);
	  } 
	String formatString(String text){ 
		if(text == null) {
			return ""; 
		}
		return text;
	}
}