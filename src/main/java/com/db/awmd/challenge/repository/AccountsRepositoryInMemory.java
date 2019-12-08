package com.db.awmd.challenge.repository;

import com.db.awmd.challenge.domain.Account;
import com.db.awmd.challenge.domain.BalanceTransferRequest;
import com.db.awmd.challenge.exception.DuplicateAccountIdException;
import com.db.awmd.challenge.exception.InsufficientAmountException;
import com.db.awmd.challenge.exception.InvalidAccountIdException;
import com.db.awmd.challenge.service.NotificationService;

import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository
@Slf4j
public class AccountsRepositoryInMemory implements AccountsRepository {

  private final Map<String, Account> accounts = new ConcurrentHashMap<>();
  
  @Autowired
  private NotificationService notificationService;

  @Override
  public void createAccount(Account account) throws DuplicateAccountIdException {
    Account previousAccount = accounts.putIfAbsent(account.getAccountId(), account);
    if (previousAccount != null) {
      throw new DuplicateAccountIdException(
        "Account id " + account.getAccountId() + " already exists!");
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
 
  @Override
  public boolean transferMoney(final BalanceTransferRequest balanceTransferRequest) throws InvalidAccountIdException{
	
	  if(balanceTransferRequest.getAccountFromId().trim().equals(balanceTransferRequest.getAccountToId().trim())){
		  throw new InvalidAccountIdException("From and To accounts are same.");
	  }
	  
	  if(!accounts.containsKey(balanceTransferRequest.getAccountFromId().trim())){
		  throw new InvalidAccountIdException(balanceTransferRequest.getAccountFromId()
		  		+ " account does not exists!");
	  }
	  
	  if(!accounts.containsKey(balanceTransferRequest.getAccountToId().trim())){
		  throw new InvalidAccountIdException(balanceTransferRequest.getAccountToId()
		  		+ " account does not exists!");
	  }
	  
	  final Account fromAccount= accounts.compute(balanceTransferRequest.getAccountFromId().trim(), (accountNo,account)->{
		    if(account.getBalance().doubleValue()>=balanceTransferRequest.getAmount().doubleValue()) {
		    	account.setBalance(account.getBalance().subtract(balanceTransferRequest.getAmount()));
		    }else throw new InsufficientAmountException(
			  		 "Your account does not have sufficent balance.");
	        return account;
	  });
	  
	  final Account toAccount;
	  try {
		    toAccount=  accounts.compute(balanceTransferRequest.getAccountToId().trim(), (accountNo,account)->{
		    account.setBalance(account.getBalance().add(balanceTransferRequest.getAmount()));
	        return account;
	        });
	  }catch(IllegalStateException ise) {
		  log.error("Error during crediting in "+balanceTransferRequest.getAccountToId()+". Rolling back in "+balanceTransferRequest.getAccountFromId()+" Amount "+balanceTransferRequest.getAmount());
		  accounts.compute(balanceTransferRequest.getAccountFromId().trim(), (accountNo,account)->{
			    	account.setBalance(account.getBalance().add(balanceTransferRequest.getAmount()));
		        return account;
		  });
		  throw ise;
	  }
	  
	  CompletableFuture.runAsync(() -> {
		  notificationService.notifyAboutTransfer(fromAccount, "Your account debited with "+balanceTransferRequest.getAmount()+" amount. Now available balance is "+fromAccount.getBalance()+".");
		  notificationService.notifyAboutTransfer(toAccount, "Your account credited with "+balanceTransferRequest.getAmount()+" amount. Now available balance is "+toAccount.getBalance()+".");
		});
	  return true;
  }
}
