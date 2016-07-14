package cn.com.open.openpaas.payservice.app.balance.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import cn.com.open.openpaas.payservice.app.balance.model.UserAccountBalance;
import cn.com.open.openpaas.payservice.app.infrastructure.repository.MerchantInfoRepository;
import cn.com.open.openpaas.payservice.app.infrastructure.repository.UserAccountBalanceRepository;
import cn.com.open.openpaas.payservice.app.merchant.model.MerchantInfo;

/**
 * 
 */
@Service("userAccountBalanceService")
public class UserAccountBalanceServiceImpl implements UserAccountBalanceService {

    @Autowired
    private UserAccountBalanceRepository userAccountBalanceRepository;

	@Override
	public void saveUserAccountBalance(UserAccountBalance userAccountBalance) {
		userAccountBalanceRepository.saveUserAccountBalance(userAccountBalance);
		
	}

	@Override
	public UserAccountBalance findByUserId(String userId) {
		// TODO Auto-generated method stub
		return userAccountBalanceRepository.findByUserId(userId);
	}

	@Override
	public void updateBalanceInfo(UserAccountBalance userAccountBalance) {
		userAccountBalanceRepository.updateBalanceInfo(userAccountBalance);
		
	}

	@Override
	public UserAccountBalance getBalanceInfo(String sourceId, Integer appId) {
		// TODO Auto-generated method stub
		return userAccountBalanceRepository.getBalanceInfo(sourceId, appId);
	}


}