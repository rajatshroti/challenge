package com.db.awmd.challenge.service;

import com.db.awmd.challenge.domain.Account;
import com.db.awmd.challenge.domain.BalanceTransferRequest;
import com.db.awmd.challenge.exception.InsufficientAmountException;
import com.db.awmd.challenge.exception.InvalidAccountIdException;
import com.db.awmd.challenge.repository.AccountsRepository;
import com.db.awmd.challenge.web.AccountsController;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CompletableFuture;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class AccountsService {

	@Getter
	private final AccountsRepository accountsRepository;

	@Getter
	private final NotificationService notificationService;

	@Autowired
	public AccountsService(AccountsRepository accountsRepository, NotificationService notificationService) {
		this.accountsRepository = accountsRepository;
		this.notificationService = notificationService;

	}

	public void createAccount(Account account) {
		this.accountsRepository.createAccount(account);
	}

	public Account getAccount(String accountId) {
		return this.accountsRepository.getAccount(accountId);
	}

	public boolean transferMoney(final BalanceTransferRequest balanceTransferRequest)
			throws InvalidAccountIdException, InsufficientAmountException {
		log.info("In transferMoney with request {}", balanceTransferRequest);
		if (balanceTransferRequest.getAccountFromId().equals(balanceTransferRequest.getAccountToId()))
			throw new InvalidAccountIdException("From and To accounts are same.");
		if (balanceTransferRequest.getAmount().doubleValue() < 0)
			throw new InsufficientAmountException(
					balanceTransferRequest.getAmount() + " not a valid amount to transfer.");
		final Account fromAccount = this.accountsRepository.getAccount(balanceTransferRequest.getAccountFromId());
		if (fromAccount == null)
			throw new InvalidAccountIdException(
					balanceTransferRequest.getAccountFromId() + " account does not exists!");
		final Account toAccount = this.accountsRepository.getAccount(balanceTransferRequest.getAccountToId());
		if (toAccount == null)
			throw new InvalidAccountIdException(balanceTransferRequest.getAccountToId() + " account does not exists!");

		synchronized (getAccountForLock(fromAccount, toAccount, true)) {
			synchronized (getAccountForLock(fromAccount, toAccount, false)) {
				try {
					this.accountsRepository.debitAccount(fromAccount, balanceTransferRequest.getAmount());
					notifyUser(fromAccount, "Your account debited with " + balanceTransferRequest.getAmount()
							+ " amount. Now available balance is " + fromAccount.getBalance() + ".");
				} catch (InsufficientAmountException iae) {
					log.error("Insufficient Amount Exception while debiting from account " + fromAccount
							+ " with amount of " + balanceTransferRequest.getAmount(), iae);
					throw iae;
				}
				this.accountsRepository.creditAccount(toAccount, balanceTransferRequest.getAmount());
				notifyUser(toAccount, "Your account credited with " + balanceTransferRequest.getAmount()
						+ " amount. Now available balance is " + toAccount.getBalance() + ".");
				return true;

			}
		}
	}

	// Method to get account object for locking
	private Account getAccountForLock(final Account one, final Account two, boolean wantSmall) {
		if (wantSmall) {
			if (one.getAccountId().compareTo(two.getAccountId()) < 0) {
				return one;
			} else {
				return two;
			}
		} else {
			if (one.getAccountId().compareTo(two.getAccountId()) > 0) {
				return one;
			} else {
				return two;
			}
		}
	}

	private void notifyUser(Account account, String message) {
		CompletableFuture.runAsync(() -> {
			notificationService.notifyAboutTransfer(account, message);
		});
	}
}
