package com.db.awmd.challenge;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

import com.db.awmd.challenge.domain.Account;
import com.db.awmd.challenge.domain.BalanceTransferRequest;
import com.db.awmd.challenge.exception.DuplicateAccountIdException;
import com.db.awmd.challenge.exception.InsufficientAmountException;
import com.db.awmd.challenge.exception.InvalidAccountIdException;
import com.db.awmd.challenge.service.AccountsService;
import java.math.BigDecimal;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
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

	    String uniqueAccountIdFrom= "Id-101";
	    String uniqueAccountIdTo= "Id-102";
	    Account accountFrom = new Account(uniqueAccountIdFrom, new BigDecimal("525.45"));
	    Account accountTo = new Account(uniqueAccountIdTo, new BigDecimal("223.30"));
	    this.accountsService.createAccount(accountFrom);
	    this.accountsService.createAccount(accountTo);
	    BalanceTransferRequest balanceTransferRequest= new BalanceTransferRequest(uniqueAccountIdFrom, uniqueAccountIdTo, new BigDecimal("125.45"));
        assertThat(this.accountsService.transferMoney(balanceTransferRequest)).isEqualTo(true);
  }
  @Test
  public void transferMoney_failOnGreaterAmount() throws Exception {

	    String uniqueAccountIdFrom= "Id-105";
	    String uniqueAccountIdTo= "Id-106";
	    Account accountFrom = new Account(uniqueAccountIdFrom, new BigDecimal("525.45"));
	    Account accountTo = new Account(uniqueAccountIdTo, new BigDecimal("223.30"));
	    this.accountsService.createAccount(accountFrom);
	    this.accountsService.createAccount(accountTo);
	    BalanceTransferRequest balanceTransferRequest= new BalanceTransferRequest(uniqueAccountIdFrom, uniqueAccountIdTo, new BigDecimal("625.45"));
	    try {
		    this.accountsService.transferMoney(balanceTransferRequest);
	      fail("Should have failed when transfering greater amount");
	    } catch (InsufficientAmountException ex) {
	      assertThat(ex.getMessage()).isEqualTo("Your account does not have sufficent balance.");
	    }
   }
  @Test
  public void transferMoney_failOnWrongAccountTo() throws Exception {

	    String uniqueAccountIdFrom= "Id-107";
	    String uniqueAccountIdTo= "Id-108";
	    Account accountFrom = new Account(uniqueAccountIdFrom, new BigDecimal("525.45"));
	    this.accountsService.createAccount(accountFrom);
	    BalanceTransferRequest balanceTransferRequest= new BalanceTransferRequest(uniqueAccountIdFrom, uniqueAccountIdTo, new BigDecimal("125.45"));
	    try {
		    this.accountsService.transferMoney(balanceTransferRequest);
	       fail("Should have failed when transfering to wrong account");
	    } catch (InvalidAccountIdException ex) {
	      assertThat(ex.getMessage()).isEqualTo(uniqueAccountIdTo+" account does not exists!");
	    }
   }
  @Test
  public void transferMoney_failOnWrongAccountFrom() throws Exception {

	    String uniqueAccountIdTo= "Id-107";
	    String uniqueAccountIdFrom= "Id-108";
	    Account accountTo = new Account(uniqueAccountIdTo, new BigDecimal("525.45"));
	    this.accountsService.createAccount(accountTo);
	    BalanceTransferRequest balanceTransferRequest= new BalanceTransferRequest(uniqueAccountIdFrom, uniqueAccountIdTo, new BigDecimal("125.45"));
	    try {
		    this.accountsService.transferMoney(balanceTransferRequest);
	       fail("Should have failed when transfering to wrong account");
	    } catch (InvalidAccountIdException ex) {
	      assertThat(ex.getMessage()).isEqualTo(uniqueAccountIdFrom+" account does not exists!");
	      
	    }
    }
  @Test
  public void transferMoney_failOnTransferingSameAccount() throws Exception {

	    String uniqueAccountIdTo= "Id-107";
	    String uniqueAccountIdFrom= "Id-107";
	    BalanceTransferRequest balanceTransferRequest= new BalanceTransferRequest(uniqueAccountIdFrom, uniqueAccountIdTo, new BigDecimal("125.45"));
	    try {
		    this.accountsService.transferMoney(balanceTransferRequest);
	       fail("Should have failed when transfering to wrong account");
	    } catch (InvalidAccountIdException ex) {
	      assertThat(ex.getMessage()).isEqualTo("From and To accounts are same.");
	    }
    }
}
