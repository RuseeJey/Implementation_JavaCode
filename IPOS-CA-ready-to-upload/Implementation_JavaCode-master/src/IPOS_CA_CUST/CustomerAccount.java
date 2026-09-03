package IPOS_CA_CUST;

import java.time.LocalDate;
import java.util.UUID;

public class CustomerAccount {

	private String customerID;
	private String name;
	private String address;
	private String phone;
	private double creditLimit;
	private double currentBalance;
	private String accountStatus;       // "normal", "suspended", "in default"
	private String discountPlan;        // stores planID value
	private String status1stReminder;   // "no_need", "due", "sent"
	private String status2ndReminder;   // "no_need", "due", "sent"
	private LocalDate date1stReminder;
	private LocalDate date2ndReminder;
	private LocalDate lastPaymentDate;
	private String periodDebtCleared;   // "YYYY-MM"

	public CustomerAccount() {}

	// Used when creating a brand new account from the UI
	public CustomerAccount(String name, String address, double creditLimit) {
		this.customerID        = UUID.randomUUID().toString();
		this.name              = name;
		this.address           = address;
		this.phone             = "";
		this.creditLimit       = creditLimit;
		this.currentBalance    = 0.0;
		this.accountStatus     = "normal";
		this.status1stReminder = "no_need";
		this.status2ndReminder = "no_need";
		this.discountPlan      = "";
	}

	// Used when loading from DB (6-arg)
	public CustomerAccount(String customerID, String name, String address,
						   double creditLimit, double currentBalance, String accountStatus) {
		this.customerID        = customerID;
		this.name              = name;
		this.address           = address;
		this.phone             = "";
		this.creditLimit       = creditLimit;
		this.currentBalance    = currentBalance;
		this.accountStatus     = accountStatus;
		this.status1stReminder = "no_need";
		this.status2ndReminder = "no_need";
		this.discountPlan      = "";
	}

	// 7-arg constructor with discount plan
	public CustomerAccount(String customerID, String name, String address,
						   double creditLimit, double currentBalance,
						   String accountStatus, String discountPlan) {
		this(customerID, name, address, creditLimit, currentBalance, accountStatus);
		this.discountPlan = discountPlan;
	}

	// ── Business logic ────────────────────────────────────────────────────────

	/** Returns true if this account can make a purchase of the given amount */
	public boolean canMakePurchase(double purchaseAmount) {
		if ("in default".equals(accountStatus)) return false;
		return (currentBalance + purchaseAmount) <= creditLimit;
	}

	/** Account holders must always pay by card per spec */
	public boolean isCashAllowed() { return false; }

	// ── Getters ───────────────────────────────────────────────────────────────

	public String    getCustomerID()          { return customerID; }
	public String    getName()                { return name; }
	public String    getAddress()             { return address; }
	public String    getPhone()               { return phone; }
	public double    getCreditLimit()         { return creditLimit; }
	public double    getCurrentBalance()      { return currentBalance; }
	public String    getAccountStatus()       { return accountStatus; }
	public String    getDiscountPlan()        { return discountPlan; }
	public String    getPlanID()              { return discountPlan; }  // alias
	public String    getStatus1stReminder()   { return status1stReminder; }
	public String    getStatus2ndReminder()   { return status2ndReminder; }
	public LocalDate getDate1stReminder()     { return date1stReminder; }
	public LocalDate getDate2ndReminder()     { return date2ndReminder; }
	public LocalDate getLastPaymentDate()     { return lastPaymentDate; }
	public String    getPeriodDebtCleared()   { return periodDebtCleared; }

	// ── Setters ───────────────────────────────────────────────────────────────

	public void setCustomerID(String v)             { customerID = v; }
	public void setName(String v)                   { name = v; }
	public void setAddress(String v)                { address = v; }
	public void setPhone(String v)                  { phone = v; }
	public void setCreditLimit(double v)            { creditLimit = v; }
	public void setCurrentBalance(double v)         { currentBalance = v; }
	public void setAccountStatus(String v)          { accountStatus = v; }
	public void setDiscountPlan(String v)           { discountPlan = v; }
	public void setPlanID(String v)                 { discountPlan = v; }  // alias
	public void setStatus1stReminder(String v)      { status1stReminder = v; }
	public void setStatus2ndReminder(String v)      { status2ndReminder = v; }
	public void setDate1stReminder(LocalDate v)     { date1stReminder = v; }
	public void setDate2ndReminder(LocalDate v)     { date2ndReminder = v; }
	public void setLastPaymentDate(LocalDate v)     { lastPaymentDate = v; }
	public void setPeriodDebtCleared(String v)      { periodDebtCleared = v; }

	@Override
	public String toString() {
		return customerID + " — " + name + " [" + accountStatus + "] £" +
				String.format("%.2f", currentBalance);
	}
}