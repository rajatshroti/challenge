package com.db.awmd.challenge;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.db.awmd.challenge.domain.Account;
import com.db.awmd.challenge.domain.BalanceTransferRequest;
import com.db.awmd.challenge.exception.DuplicateAccountIdException;
import com.db.awmd.challenge.exception.InsufficientAmountException;
import com.db.awmd.challenge.exception.InvalidAccountIdException;
import com.db.awmd.challenge.service.AccountsService;
import com.db.awmd.challenge.service.EmailNotificationService;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

import java.math.BigDecimal;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
public class AccountsServiceTest {

	@Autowired
	private AccountsService accountsService;

	@Before
	public void prepareMockService() {
		// Reset the existing accounts before each test.
		accountsService.getAccountsRepository().clearAccounts();
	}

	@Test
	public void addAccount() throws Exception {
		Account account = new Account("Id-123");
		account.setBalance(new BigDecimal(1000));
		this.accountsService.createAccount(account);

		assertThat(this.accountsService.getAccount("Id-123")).isEqualTo(account);
	}

	@Test
	public void addAccount_failsOnDuplicateId() throws Exception {
		String uniqueId = "Id-" + System.currentTimeMillis();
		Account account = new Account(uniqueId);
		this.accountsService.createAccount(account);

		try {
			this.accountsService.createAccount(account);
			fail("Should have failed when adding duplicate account");
		} catch (DuplicateAccountIdException ex) {
			assertThat(ex.getMessage()).isEqualTo("Account id " + uniqueId + " already exists!");
		}

	}

	@Test
	public void transferMoney() throws Exception {
		String uniqueAccountIdFrom = "Id-101";
		String uniqueAccountIdTo = "Id-102";
		Account accountFrom = new Account(uniqueAccountIdFrom, new BigDecimal("525.45"));
		Account accountTo = new Account(uniqueAccountIdTo, new BigDecimal("223.30"));
		this.accountsService.createAccount(accountFrom);
		this.accountsService.createAccount(accountTo);
		BalanceTransferRequest balanceTransferRequest = new BalanceTransferRequest(uniqueAccountIdFrom,
				uniqueAccountIdTo, new BigDecimal("125.45"));

		assertThat(this.accountsService.transferMoney(balanceTransferRequest)).isEqualTo(true);
		assertThat(this.accountsService.getAccount(uniqueAccountIdFrom).getBalance())
				.isEqualTo(new BigDecimal("400.00"));
		assertThat(this.accountsService.getAccount(uniqueAccountIdTo).getBalance()).isEqualTo(new BigDecimal("348.75"));

	}

	@Test
	public void NotificationServiceWithTransferMoney() throws Exception {
		Logger notificationLogger = (Logger) LoggerFactory.getLogger(EmailNotificationService.class);
		ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
		listAppender.start();
		notificationLogger.addAppender(listAppender);
		String uniqueAccountIdFrom = "Id-101";
		String uniqueAccountIdTo = "Id-102";
		Account accountFrom = new Account(uniqueAccountIdFrom, new BigDecimal("525.45"));
		Account accountTo = new Account(uniqueAccountIdTo, new BigDecimal("223.30"));
		this.accountsService.createAccount(accountFrom);
		this.accountsService.createAccount(accountTo);
		BalanceTransferRequest balanceTransferRequest = new BalanceTransferRequest(uniqueAccountIdFrom,
				uniqueAccountIdTo, new BigDecimal("125.45"));

		this.accountsService.transferMoney(balanceTransferRequest);
		this.accountsService.getAccount(uniqueAccountIdFrom);
		this.accountsService.getAccount(uniqueAccountIdTo);
		List<ILoggingEvent> logsList = listAppender.list;
		final List<String> loggerMessageList = new LinkedList<>();
		logsList.forEach(message -> {
			loggerMessageList.add(message.toString());
		});
		assertTrue(loggerMessageList.contains(String.format("[INFO] Sending notification to owner of %s: %s",
				uniqueAccountIdTo, "Your account credited with 125.45 amount. Now available balance is 348.75.")));
		assertTrue(loggerMessageList.contains(String.format("[INFO] Sending notification to owner of %s: %s",
				uniqueAccountIdFrom, "Your account debited with 125.45 amount. Now available balance is 400.00.")));

	}

	@Test
	public void transferMoney_failOnGreaterAmount() throws Exception {

		String uniqueAccountIdFrom = "Id-105";
		String uniqueAccountIdTo = "Id-106";
		Account accountFrom = new Account(uniqueAccountIdFrom, new BigDecimal("525.45"));
		Account accountTo = new Account(uniqueAccountIdTo, new BigDecimal("223.30"));
		this.accountsService.createAccount(accountFrom);
		this.accountsService.createAccount(accountTo);
		BalanceTransferRequest balanceTransferRequest = new BalanceTransferRequest(uniqueAccountIdFrom,
				uniqueAccountIdTo, new BigDecimal("625.45"));
		try {
			this.accountsService.transferMoney(balanceTransferRequest);
			fail("Should have failed when transfering greater amount");
		} catch (InsufficientAmountException ex) {
			assertThat(ex.getMessage()).isEqualTo(uniqueAccountIdFrom + " account does not have sufficent balance.");
		}
	}

	@Test
	public void transferMoney_failOnWrongAccountTo() throws Exception {

		String uniqueAccountIdFrom = "Id-107";
		String uniqueAccountIdTo = "Id-108";
		Account accountFrom = new Account(uniqueAccountIdFrom, new BigDecimal("525.45"));
		this.accountsService.createAccount(accountFrom);
		BalanceTransferRequest balanceTransferRequest = new BalanceTransferRequest(uniqueAccountIdFrom,
				uniqueAccountIdTo, new BigDecimal("125.45"));
		try {
			this.accountsService.transferMoney(balanceTransferRequest);
			fail("Should have failed when transfering to wrong account");
		} catch (InvalidAccountIdException ex) {
			assertThat(ex.getMessage()).isEqualTo(uniqueAccountIdTo + " account does not exists!");
		}
	}

	@Test
	public void transferMoney_failOnWrongAccountFrom() throws Exception {

		String uniqueAccountIdTo = "Id-107";
		String uniqueAccountIdFrom = "Id-108";
		Account accountTo = new Account(uniqueAccountIdTo, new BigDecimal("525.45"));
		this.accountsService.createAccount(accountTo);
		BalanceTransferRequest balanceTransferRequest = new BalanceTransferRequest(uniqueAccountIdFrom,
				uniqueAccountIdTo, new BigDecimal("125.45"));
		try {
			this.accountsService.transferMoney(balanceTransferRequest);
			fail("Should have failed when transfering to wrong account");
		} catch (InvalidAccountIdException ex) {
			assertThat(ex.getMessage()).isEqualTo(uniqueAccountIdFrom + " account does not exists!");

		}
	}

	@Test
	public void transferMoney_failOnTransferingSameAccount() throws Exception {

		String uniqueAccountIdTo = "Id-107";
		String uniqueAccountIdFrom = "Id-107";
		BalanceTransferRequest balanceTransferRequest = new BalanceTransferRequest(uniqueAccountIdFrom,
				uniqueAccountIdTo, new BigDecimal("125.45"));
		try {
			this.accountsService.transferMoney(balanceTransferRequest);
			fail("Should have failed when transfering to wrong account");
		} catch (InvalidAccountIdException ex) {
			assertThat(ex.getMessage()).isEqualTo("From and To accounts are same.");
		}
	}

	@Test
	public void transferMoney_negativeAmount() throws Exception {

		String uniqueAccountIdFrom = "Id-105";
		String uniqueAccountIdTo = "Id-106";
		String amount = "-125.45";
		Account accountFrom = new Account(uniqueAccountIdFrom, new BigDecimal("525.45"));
		Account accountTo = new Account(uniqueAccountIdTo, new BigDecimal("223.30"));
		this.accountsService.createAccount(accountFrom);
		this.accountsService.createAccount(accountTo);
		BalanceTransferRequest balanceTransferRequest = new BalanceTransferRequest(uniqueAccountIdFrom,
				uniqueAccountIdTo, new BigDecimal(amount));
		try {
			this.accountsService.transferMoney(balanceTransferRequest);
			fail("Should have failed when amount is not valid");
		} catch (InsufficientAmountException ex) {
			assertThat(ex.getMessage()).isEqualTo(amount + " not a valid amount to transfer.");
		}
	}

	@Test
	public void transferMoneyWithMultiThread() throws Exception {
		String uniqueAccountIdOne = "Id-101";
		String uniqueAccountIdTwo = "Id-102";
		Account accountOne = new Account(uniqueAccountIdOne, new BigDecimal("525.45"));
		Account accountTwo = new Account(uniqueAccountIdTwo, new BigDecimal("223.30"));
		this.accountsService.createAccount(accountOne);
		this.accountsService.createAccount(accountTwo);
		Thread t1 = new Thread() {
			public void run() {
				BalanceTransferRequest balanceTransferRequest = new BalanceTransferRequest(uniqueAccountIdOne,
						uniqueAccountIdTwo, new BigDecimal("125.45"));
				assertThat(accountsService.transferMoney(balanceTransferRequest)).isEqualTo(true);
				assertThat(accountsService.getAccount(uniqueAccountIdOne).getBalance())
						.isEqualTo(new BigDecimal("400.00"));
				assertThat(accountsService.getAccount(uniqueAccountIdTwo).getBalance())
						.isEqualTo(new BigDecimal("348.75"));

			}
		};
		Thread t2 = new Thread() {
			public void run() {
				BalanceTransferRequest balanceTransferRequest = new BalanceTransferRequest(uniqueAccountIdTwo,
						uniqueAccountIdOne, new BigDecimal("125.45"));
				assertThat(accountsService.transferMoney(balanceTransferRequest)).isEqualTo(true);
				assertThat(accountsService.getAccount(uniqueAccountIdOne).getBalance())
						.isEqualTo(new BigDecimal("525.45"));
				assertThat(accountsService.getAccount(uniqueAccountIdTwo).getBalance())
						.isEqualTo(new BigDecimal("223.30"));
			}
		};
		ExecutorService executor = Executors.newFixedThreadPool(2);
		executor.submit(t1);
		executor.submit(t2);
	}

}
