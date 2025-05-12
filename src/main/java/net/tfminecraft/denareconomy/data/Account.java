package net.tfminecraft.DenarEconomy.Data;

import java.math.BigDecimal;

public class Account {
	private BigDecimal amount;
	private boolean taxable = true;
	
	public Account(double amount) {
		this.amount = BigDecimal.valueOf(amount);
	}
	
	public Account(double amount, boolean tax) {
		this.amount = BigDecimal.valueOf(amount);
		this.taxable = tax;
	}
	
	public double getBal() {
		return amount.doubleValue();
	}
	
	public void setBal(double amount) {
		this.amount = BigDecimal.valueOf(amount);
	}
	
	public void change(double a) {
		BigDecimal change = BigDecimal.valueOf(a);
		amount = amount.add(change);
	}

	public boolean isTaxable(){
		return taxable;
	}
}
