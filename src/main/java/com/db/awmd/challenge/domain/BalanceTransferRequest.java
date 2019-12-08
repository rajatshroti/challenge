package com.db.awmd.challenge.domain;

import java.math.BigDecimal;

import javax.validation.constraints.Min;

import org.hibernate.validator.constraints.NotEmpty;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;
import lombok.NonNull;

@Data
public class BalanceTransferRequest {

	@NonNull
	@NotEmpty
	private String accountFromId;
	
	@NonNull
	@NotEmpty
	private String accountToId;
	
	@Min(value = 1, message = "Amount must be greater then 1.")
	private BigDecimal amount;
	
	 @JsonCreator
	  public BalanceTransferRequest(@JsonProperty("accountFromId") String accountFromId,
	    @JsonProperty("accountToId") String accountToId,  @JsonProperty("amount") BigDecimal amount ) {
	    this.accountFromId = accountFromId;
	    this.accountToId = accountToId;
	    this.amount = amount;
	  }
	
}
