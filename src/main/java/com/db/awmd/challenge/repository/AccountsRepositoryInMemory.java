package com.db.awmd.challenge.repository;

import com.db.awmd.challenge.domain.Account;
import com.db.awmd.challenge.domain.BalanceTransferRequest;
import com.db.awmd.challenge.exception.DuplicateAccountIdException;
import com.db.awmd.challenge.exception.InsufficientAmountException;
import com.db.awmd.challenge.exception.InvalidAccountIdException;
import com.db.awmd.challenge.service.NotificationService;

import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository
public class AccountsRepositoryInMemory implements AccountsRepository {

	private final Map<String, Account> accounts = new ConcurrentHashMap<>();

	@Override
	public void createAccount(Account account) throws DuplicateAccountIdException {
		Account previousAccount = accounts.putIfAbsent(account.getAccountId(), account);
		if (previousAccount != null) {
			throw new DuplicateAccountIdException("Account id " + account.getAccountId() + " already exists!");
		}
	}

	@Override
	public Account getAccount(String accountId) {
		return accounts.get(accountId);
	}

	@Override
	public void clearAccounts() {
		accounts.clear();
	}

	
	public Account debitAccount(final Account account, final BigDecimal amount) {
		accounts.compute(account.getAccountId().trim(), (accountNo, accountObj) -> {
			if (account.getBalance().doubleValue() >= amount.doubleValue()) {
				account.setBalance(account.getBalance().subtract(amount));
			} else
				throw new InsufficientAmountException(account.getAccountId()+" account does not have sufficent balance.");
			return account;
		});
		return account;
	}

	public Account creditAccount(final Account account, final BigDecimal amount) {
		 accounts.compute(account.getAccountId(),(accountNo,accountObj)->{
		  account.setBalance(account.getBalance().add(amount));
		  return account;
		  });
 		return account;
	}
}
