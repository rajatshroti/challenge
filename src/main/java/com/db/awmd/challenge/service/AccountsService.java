package com.db.awmd.challenge.service;

import com.db.awmd.challenge.domain.Account;
import com.db.awmd.challenge.domain.BalanceTransferRequest;
import com.db.awmd.challenge.exception.InsufficientAmountException;
import com.db.awmd.challenge.exception.InvalidAccountIdException;
import com.db.awmd.challenge.repository.AccountsRepository;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AccountsService {

  @Getter
  private final AccountsRepository accountsRepository;
  
  @Autowired
  public AccountsService(AccountsRepository accountsRepository) {
    this.accountsRepository = accountsRepository;
  }

  public void createAccount(Account account) {
    this.accountsRepository.createAccount(account);
  }

  public Account getAccount(String accountId) {
    return this.accountsRepository.getAccount(accountId);
  }
  public boolean transferMoney(final BalanceTransferRequest balanceTransferRequest) throws InvalidAccountIdException, InsufficientAmountException {
    return this.accountsRepository.transferMoney(balanceTransferRequest);
   }
}
