package cn.com.open.openpaas.payservice.app.order.service;

import cn.com.open.openpaas.payservice.app.order.model.PayLoanInfo;

public interface PayLoanInfoService {
	public Boolean savePayLoanInfo(PayLoanInfo payLoanInfo); 
	public PayLoanInfo findByOrderId(String id); 
	public Boolean updateStatus(Integer id,Integer status);
	


}
