package cn.com.open.openpaas.payservice.app.channel.tclpay.config;

import cn.com.open.openpaas.payservice.app.tools.PropertiesTool;

/**
 * 名称：测试公共配置类
 * 版本：1.1 
 * 日期：2015-07 
 * 作者：深圳市前海汇银通支付科技有限公司技术管理部 
 * 版权：深圳市前海汇银通支付科技有限公司
 * 说明：以下代码只是为了方便商户测试而提供的样例代码，商户可以根据自己网站的需要，按照技术文档编写,并非一定要使用该代码。该代码仅供参考。
 * */

public class CommonConfig {

	public static String HYT_SERVICE_URL = PropertiesTool.getAppPropertieByKey("tcl_pay_url");

	/**
	 * 生产测试商户号
	 */
	public static String MERCHANT_CODE=PropertiesTool.getAppPropertieByKey("MERCHANT_CODE");
	
	
	/**
	 * 生产测试商户证书密码
	 */
	public static String MERCHANT_CERT_PWD=PropertiesTool.getAppPropertieByKey("MERCHANT_CERT_PWD");
	/**
	 * 生产测试商户公钥证书名称
	 */
	public static String MERCHANT_PUBCERT_PATH=PropertiesTool.getAppPropertieByKey("MERCHANT_PUBCERT_PATH");
    
}
