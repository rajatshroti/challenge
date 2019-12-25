package com.db.awmd.challenge;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

import com.db.awmd.challenge.domain.Account;
import com.db.awmd.challenge.domain.BalanceTransferRequest;
import com.db.awmd.challenge.service.AccountsService;
import java.math.BigDecimal;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;

@RunWith(SpringRunner.class)
@SpringBootTest
@WebAppConfiguration
public class AccountsControllerTest {

	private MockMvc mockMvc;

	@Autowired
	private AccountsService accountsService;

	@Autowired
	private WebApplicationContext webApplicationContext;

	@Before
	public void prepareMockMvc() {
		this.mockMvc = webAppContextSetup(this.webApplicationContext).build();

		// Reset the existing accounts before each test.
		accountsService.getAccountsRepository().clearAccounts();
	}

	@Test
	public void createAccount() throws Exception {
		this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
				.content("{\"accountId\":\"Id-123\",\"balance\":1000}")).andExpect(status().isCreated());

		Account account = accountsService.getAccount("Id-123");
		assertThat(account.getAccountId()).isEqualTo("Id-123");
		assertThat(account.getBalance()).isEqualByComparingTo("1000");
	}

	@Test
	public void createDuplicateAccount() throws Exception {
		this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
				.content("{\"accountId\":\"Id-123\",\"balance\":1000}")).andExpect(status().isCreated());

		this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
				.content("{\"accountId\":\"Id-123\",\"balance\":1000}")).andExpect(status().isBadRequest());
	}

	@Test
	public void createAccountNoAccountId() throws Exception {
		this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON).content("{\"balance\":1000}"))
				.andExpect(status().isBadRequest());
	}

	@Test
	public void createAccountNoBalance() throws Exception {
		this.mockMvc.perform(
				post("/v1/accounts").contentType(MediaType.APPLICATION_JSON).content("{\"accountId\":\"Id-123\"}"))
				.andExpect(status().isBadRequest());
	}

	@Test
	public void createAccountNoBody() throws Exception {
		this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isBadRequest());
	}

	@Test
	public void createAccountNegativeBalance() throws Exception {
		this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
				.content("{\"accountId\":\"Id-123\",\"balance\":-1000}")).andExpect(status().isBadRequest());
	}

	@Test
	public void createAccountEmptyAccountId() throws Exception {
		this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
				.content("{\"accountId\":\"\",\"balance\":1000}")).andExpect(status().isBadRequest());
	}

	@Test
	public void getAccount() throws Exception {
		String uniqueAccountId = "Id-" + System.currentTimeMillis();
		Account account = new Account(uniqueAccountId, new BigDecimal("123.45"));
		this.accountsService.createAccount(account);
		this.mockMvc.perform(get("/v1/accounts/" + uniqueAccountId)).andExpect(status().isOk())
				.andExpect(content().string("{\"accountId\":\"" + uniqueAccountId + "\",\"balance\":123.45}"));
	}

	// Tests added By Rajat
	@Test
	public void transferMoney() throws Exception {
		String uniqueAccountIdFrom = "Id-101";
		String uniqueAccountIdTo = "Id-102";
		Account accountFrom = new Account(uniqueAccountIdFrom, new BigDecimal(500));
		Account accountTo = new Account(uniqueAccountIdTo, new BigDecimal(300));
		this.accountsService.createAccount(accountFrom);
		this.accountsService.createAccount(accountTo);
		this.mockMvc
				.perform(post("/v1/accounts/transferMoney").contentType(MediaType.APPLICATION_JSON)
						.content("{\"accountFromId\":\"Id-101\",\"accountToId\":\"Id-102\",\"amount\":300}"))
				.andExpect(status().isAccepted());
		this.mockMvc.perform(get("/v1/accounts/" + uniqueAccountIdTo)).andExpect(status().isOk())
				.andExpect(content().string("{\"accountId\":\"" + uniqueAccountIdTo + "\",\"balance\":600}"));
		this.mockMvc.perform(get("/v1/accounts/" + uniqueAccountIdFrom)).andExpect(status().isOk())
				.andExpect(content().string("{\"accountId\":\"" + uniqueAccountIdFrom + "\",\"balance\":200}"));

	}

	@Test
	public void getAccountAfterCredit() throws Exception {
		String uniqueAccountIdFrom = "Id-101";
		String uniqueAccountIdTo = "Id-102";
		Account accountFrom = new Account(uniqueAccountIdFrom, new BigDecimal("525.45"));
		Account accountTo = new Account(uniqueAccountIdTo, new BigDecimal("223.30"));
		this.accountsService.createAccount(accountFrom);
		this.accountsService.createAccount(accountTo);
		BalanceTransferRequest balanceTransferRequest = new BalanceTransferRequest(uniqueAccountIdFrom,
				uniqueAccountIdTo, new BigDecimal("125.45"));
		this.accountsService.transferMoney(balanceTransferRequest);
		this.mockMvc.perform(get("/v1/accounts/" + uniqueAccountIdTo)).andExpect(status().isOk())
				.andExpect(content().string("{\"accountId\":\"" + uniqueAccountIdTo + "\",\"balance\":348.75}"));
	}

	@Test
	public void getAccountAfterDebit() throws Exception {
		String uniqueAccountIdFrom = "Id-101";
		String uniqueAccountIdTo = "Id-102";
		Account accountFrom = new Account(uniqueAccountIdFrom, new BigDecimal("525.45"));
		Account accountTo = new Account(uniqueAccountIdTo, new BigDecimal("223.30"));
		this.accountsService.createAccount(accountFrom);
		this.accountsService.createAccount(accountTo);
		BalanceTransferRequest balanceTransferRequest = new BalanceTransferRequest(uniqueAccountIdFrom,
				uniqueAccountIdTo, new BigDecimal("125.45"));
		this.accountsService.transferMoney(balanceTransferRequest);
		this.mockMvc.perform(get("/v1/accounts/" + uniqueAccountIdFrom)).andExpect(status().isOk())
				.andExpect(content().string("{\"accountId\":\"" + uniqueAccountIdFrom + "\",\"balance\":400.00}"));
	}

	@Test
	public void transferMoneyToWrongAccountTo() throws Exception {
		String uniqueAccountIdFrom = "Id-101";
		Account accountFrom = new Account(uniqueAccountIdFrom, new BigDecimal("525.45"));
		this.accountsService.createAccount(accountFrom);
		this.mockMvc
				.perform(post("/v1/accounts/transferMoney").contentType(MediaType.APPLICATION_JSON)
						.content("{\"accountFromId\":\"Id-101\",\"accountToId\":\"Id-103\",\"amount\":125.45}"))
				.andExpect(status().isBadRequest()).andExpect(content().string("Id-103 account does not exists!"));

	}

	@Test
	public void transferMoneyToWrongAccountFrom() throws Exception {
		String uniqueAccountIdTo = "Id-101";
		Account accountTo = new Account(uniqueAccountIdTo, new BigDecimal("525.45"));
		this.accountsService.createAccount(accountTo);
		this.mockMvc
				.perform(post("/v1/accounts/transferMoney").contentType(MediaType.APPLICATION_JSON)
						.content("{\"accountFromId\":\"Id-103\",\"accountToId\":\"Id-101\",\"amount\":125.45}"))
				.andExpect(status().isBadRequest()).andExpect(content().string("Id-103 account does not exists!"));

	}

	@Test
	public void transferMoneyToSameAccount() throws Exception {
		String uniqueAccountIdTo = "Id-101";
		Account accountTo = new Account(uniqueAccountIdTo, new BigDecimal("525.45"));
		this.accountsService.createAccount(accountTo);
		this.mockMvc
				.perform(post("/v1/accounts/transferMoney").contentType(MediaType.APPLICATION_JSON)
						.content("{\"accountFromId\":\"Id-101\",\"accountToId\":\"Id-101\",\"amount\":125.45}"))
				.andExpect(status().isBadRequest()).andExpect(content().string("From and To accounts are same."));

	}

	@Test
	public void transferMoneyGreaterThenBalance() throws Exception {
		String uniqueAccountIdFrom = "Id-101";
		String uniqueAccountIdTo = "Id-102";
		Account accountFrom = new Account(uniqueAccountIdFrom, new BigDecimal("525.45"));
		Account accountTo = new Account(uniqueAccountIdTo, new BigDecimal("223.30"));
		this.accountsService.createAccount(accountFrom);
		this.accountsService.createAccount(accountTo);
		this.mockMvc
				.perform(post("/v1/accounts/transferMoney").contentType(MediaType.APPLICATION_JSON)
						.content("{\"accountFromId\":\"Id-101\",\"accountToId\":\"Id-102\",\"amount\":725.45}"))
				.andExpect(status().isBadRequest())
				.andExpect(content().string(uniqueAccountIdFrom + " account does not have sufficent balance."));

	}

	@Test
	public void transferMoney_withNegativeAmount() throws Exception {
		String uniqueAccountIdFrom = "Id-101";
		String uniqueAccountIdTo = "Id-102";
		Account accountFrom = new Account(uniqueAccountIdFrom, new BigDecimal("525.45"));
		Account accountTo = new Account(uniqueAccountIdTo, new BigDecimal("223.30"));
		this.accountsService.createAccount(accountFrom);
		this.accountsService.createAccount(accountTo);
		this.mockMvc
				.perform(post("/v1/accounts/transferMoney").contentType(MediaType.APPLICATION_JSON)
						.content("{\"accountFromId\":\"Id-101\",\"accountToId\":\"Id-102\",\"amount\":-125.45}"))
				.andExpect(status().isBadRequest());

	}
}
