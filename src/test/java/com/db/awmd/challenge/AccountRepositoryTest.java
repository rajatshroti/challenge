package com.db.awmd.challenge;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

import java.math.BigDecimal;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import com.db.awmd.challenge.domain.Account;
import com.db.awmd.challenge.exception.DuplicateAccountIdException;
import com.db.awmd.challenge.exception.InsufficientAmountException;
import com.db.awmd.challenge.repository.AccountsRepository;

@RunWith(SpringRunner.class)
@SpringBootTest
public class AccountRepositoryTest {

	@Autowired
	private AccountsRepository accountsRepository;

	@Before
	public void prepareMockService() {
		// Reset the existing accounts before each test.
		accountsRepository.clearAccounts();
	}

	@Test
	public void addAccount() throws Exception {
		Account account = new Account("Id-123");
		account.setBalance(new BigDecimal(1000));
		this.accountsRepository.createAccount(account);

		assertThat(this.accountsRepository.getAccount("Id-123")).isEqualTo(account);
	}

	@Test
	public void addAccount_failsOnDuplicateId() throws Exception {
		String uniqueId = "Id-" + System.currentTimeMillis();
		Account account = new Account(uniqueId);
		this.accountsRepository.createAccount(account);

		try {
			this.accountsRepository.createAccount(account);
			fail("Should have failed when adding duplicate account");
		} catch (DuplicateAccountIdException ex) {
			assertThat(ex.getMessage()).isEqualTo("Account id " + uniqueId + " already exists!");
		}

	}

	@Test
	public void creditAccount() throws Exception {
		Account account = new Account("Id-123");
		account.setBalance(new BigDecimal(1000));
		this.accountsRepository.createAccount(account);
		assertThat(this.accountsRepository.creditAccount(account, new BigDecimal(500)).getBalance())
				.isEqualTo(new BigDecimal(1500));
	}

	@Test
	public void debitAccount() throws Exception {
		Account account = new Account("Id-123");
		account.setBalance(new BigDecimal(1000));
		this.accountsRepository.createAccount(account);
		assertThat(this.accountsRepository.debitAccount(account, new BigDecimal(500)).getBalance())
				.isEqualTo(new BigDecimal(500));
	}

	@Test
	public void debitWithGreaterAmount() throws Exception {
		Account account = new Account("Id-123");
		account.setBalance(new BigDecimal(1000));
		this.accountsRepository.createAccount(account);
		try {
			this.accountsRepository.debitAccount(account, new BigDecimal(1500));
			fail("Should have failed when adding duplicate account");
		} catch (InsufficientAmountException ex) {
			assertThat(ex.getMessage()).isEqualTo(account.getAccountId() + " account does not have sufficent balance.");
		}
	}
}
