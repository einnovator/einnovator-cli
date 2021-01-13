package org.einnovator.cli;

import static org.einnovator.util.MappingUtils.updateObjectFrom;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.ResourceBundle;

import org.einnovator.payments.client.PaymentsClient;
import org.einnovator.payments.client.config.PaymentsClientConfiguration;
import org.einnovator.payments.client.model.Account;
import org.einnovator.payments.client.model.BankAccount;
import org.einnovator.payments.client.model.Card;
import org.einnovator.payments.client.model.Payable;
import org.einnovator.payments.client.model.Payment;
import org.einnovator.payments.client.model.Tax;
import org.einnovator.payments.client.modelx.AccountFilter;
import org.einnovator.payments.client.modelx.AccountOptions;
import org.einnovator.payments.client.modelx.PaymentFilter;
import org.einnovator.payments.client.modelx.PaymentOptions;
import org.einnovator.payments.client.modelx.TaxFilter;
import org.einnovator.payments.client.modelx.TaxOptions;
import org.einnovator.util.PageOptions;
import org.einnovator.util.UriUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;


@Component
public class Payments extends CommandRunnerBase {
	public static final String PAYMENTS_DEFAULT_SERVER = "http://localhost:2060";
	public static final String PAYMENTS_MONITOR_SERVER = "http://localhost:2061";


	private static final String ACCOUNT_DEFAULT_FORMAT = "id,user.username,group.name,title";
	private static final String ACCOUNT_WIDE_FORMAT = "id,user.username,group.name,title,enabled";

	private static final String PAYABLE_DEFAULT_FORMAT = "id,name,type,group.name,userCount";
	private static final String PAYABLE_WIDE_FORMAT = "id,name,displayName,type,group.name,userCount";

	private static final String PAYMENT_DEFAULT_FORMAT = "id,username,email,status";
	private static final String PAYMENT_WIDE_FORMAT = "id,username,email,firstName,lastName,title,address.country,phone.formatted,status,enabled";

	private static final String CARD_DEFAULT_FORMAT = "id,clientId,clientSecret";
	private static final String CARD_WIDE_FORMAT = "id,clientId,clientSecret,scopes";

	private static final String BANKACCOUNT_DEFAULT_FORMAT ="id,invitee,type,owner,status";
	private static final String BANKACCOUNT_WIDE_FORMAT ="id,invitee,type,owner,status,subject";

	private static final String TAX_DEFAULT_FORMAT = "id,name,type,owner";
	private static final String TAX_WIDE_FORMAT = "id,name,type,owner,address.country";

	OAuth2AccessToken token;
	
	private PaymentsClient paymentsClient;

	private String server = PAYMENTS_DEFAULT_SERVER;

	private PaymentsClientConfiguration config = new PaymentsClientConfiguration();

	
	
	@Override
	public String getName() {
		return "payments";
	}
	
	@Override
	protected String getServer() {
		return server;
	}


	String[][] PAYMENTS_COMMANDS = c(
		c("account", "accounts", "acc"),
		c("payment", "payments", "pay"),
		c("tax", "taxes")
	);

	protected String[][] getCommands() {
		return PAYMENTS_COMMANDS;
	}

	static Map<String, String[][]> subcommands;

	static {
		Map<String, String[][]> map = new LinkedHashMap<>();
		subcommands = map;
		map.put("account", c(c("ls", "list"), c("get"), c("schema", "meta"), 
			c("create", "add"), c("update"), c("delete", "del", "rm"),
			c("help")));
		map.put("payment", c(c("ls", "list"), c("get"), c("schema", "meta"), 
			c("submit", "create", "add"), c("update"), c("delete", "del", "remove", "rm"), 
			c("help")));
		map.put("tax", c(c("ls", "list"), c("get"), c("schema", "meta"), 
			c("create", "add"), c("update"), c("delete", "del", "remove", "rm"), c("help")));
	}

	@Override
	protected Map<String, String[][]> getSubCommands() {
		return subcommands;
	}
	
	/**
	 * @deprecated Use {@link #init(Map<String, Object>,RestTemplate,boolean,ResourceBundle)} instead
	 */
	public void init(String[] cmds, Map<String, Object> options, RestTemplate template, boolean interactive, ResourceBundle bundle) {
		init(options, template, interactive, bundle);
	}

	public void init(Map<String, Object> options, RestTemplate template, boolean interactive, ResourceBundle bundle) {
		if (!init) {
			super.init(options, template, interactive, bundle);
			config.setServer(server);
			PaymentsClientConfiguration config0 = convert(options, PaymentsClientConfiguration.class);
			config0.setServer(this.server);
			updateObjectFrom(config, config0);
			if (template instanceof OAuth2RestTemplate) {
				paymentsClient = new PaymentsClient((OAuth2RestTemplate)template, config);				
			} else {
				singleuserNotSupported();
				exit(-1);
			}
			init = true;
		}
	}

	public void setEndpoints(Map<String, Object> endpoints) {
		String server = (String)endpoints.get("server");
		if (server!=null) {
			this.server = server;
			this.config.setServer(server);
		}
	}

	public void run(String type, String op, String[] cmds, String[] extra, Map<String, Object> options) {
		setLine(type, op, cmds, extra, options);
		switch (type) {
		case "help": case "":
			printUsage();
			return;
		}
		if (!setupToken()) {
			return;
		}

		switch (type) {
		case "account": case "accounts": case "acc":
			switch (op) {
			case "help": case "":
				printUsage1();
				break;
			case "get": 
				getAccount(cmds, options);
				break;
			case "ls": case "list":
				listAccounts(cmds, options);
				break;
			case "create": case "add":
				createAccount(cmds, options);
				break;
			case "update":
				updateAccount(cmds, options);
				break;
			case "delete": case "del": case "rm": case "remove":
				deleteAccount(cmds, options);
				break;
			default: 
				invalidOp(type, op);
				break;
			}
			break;
		case "payment": case "payments": case "pay":
			switch (op) {
			case "help": case "":
				printUsage1();
				break;
			case "get": 
				getPayment(cmds, options);
				break;
			case "ls": case "list":
				listPayments(cmds, options);
				break;
			case "create": case "add":
				submitPayment(cmds, options);
				break;
			case "update":
				updatePayment(cmds, options);
				break;
			case "delete": case "del": case "rm": case "remove":
				deletePayment(cmds, options);
				break;
			default: 
				invalidOp(type, op);
				break;
			}
			break;
		case "tax": case "taxes":
			switch (op) {
			case "help": case "":
				printUsage1();
				break;
			case "get": 
				getTax(cmds, options);
				break;
			case "ls": case "list":
				listTaxes(cmds, options);
				break;
			case "create": case "add":
				createTax(cmds, options);
				break;
			case "update":
				updateTax(cmds, options);
				break;
			case "delete": case "del": case "rm": case "remove":
				deleteTax(cmds, options);
				break;
			default: 
				invalidOp(type, op);
				break;
			}
			break;
		default:
			invalidOp(type);
			printUsage();
			break;
		}
	}



	public String getUsage() {
		StringBuilder sb = new StringBuilder();
		return sb.toString();
	}
	
	//
	// Accounts
	//
	
	public void listAccounts(String[] cmds, Map<String, Object> options) {
		if (isHelp2()) {
			return;
		}
		Pageable pageable = convert(options, PageOptions.class).toPageRequest();
		AccountFilter filter = convert(options, AccountFilter.class);
		debug("Account: %s %s", filter, pageable);
		Page<Account> accounts = paymentsClient.listAccounts(filter, pageable);
		print(accounts);
	}
	
	public void getAccount(String[] cmds, Map<String, Object> options) {
		if (isHelp2()) {
			return;
		}
		String accountId = argId(op, cmds);
		debug("Account: %s", accountId);
		AccountOptions options_ = convert(options, AccountOptions.class);
		Account account = paymentsClient.getAccount(accountId, options_);
		printObj(account);
	}
	
	public void createAccount(String[] cmds, Map<String, Object> options) {
		if (isHelp2()) {
			return;
		}
		Account account = convert(options, Account.class);
		account.setName(argId(op, cmds));
		AccountOptions options_ = convert(options, AccountOptions.class);
		debug("Creating Account: %s %s", account, options_);
		if (isDryrun()) {
			return;
		}
		if (isDryrun()) {
			return;
		}
		URI uri = paymentsClient.createAccount(account, options_);
		if (isEcho()) {
			debug("Account URI: %s", uri);
			String id = UriUtils.extractId(uri);
			Account account2 = paymentsClient.getAccount(id, null);
			printObj(account2);			
		}
	}

	public void updateAccount(String[] cmds, Map<String, Object> options) {
		if (isHelp2()) {
			return;
		}
		String accountId = argId(op, cmds);
		Account account = convert(options, Account.class);
		AccountOptions options_ = convert(options, AccountOptions.class);
		debug("Updating Account: %s %s %s", accountId, account, options_);
		if (isDryrun()) {
			return;
		}
		if (isDryrun()) {
			return;
		}
		paymentsClient.updateAccount(accountId, account, options_);
		if (isEcho()) {
			Account account2 = paymentsClient.getAccount(accountId, null);
			printObj(account2);			
		}
	}
	
	public void deleteAccount(String[] cmds, Map<String, Object> options) {
		if (isHelp2()) {
			return;
		}
		String accountId = argId(op, cmds);
		debug("Deleting Account: %s", accountId);
		if (isDryrun()) {
			return;
		}
		if (isDryrun()) {
			return;
		}
		paymentsClient.deleteAccount(accountId, null);		
		if (isEcho()) {
			listAccounts(cmds, options);
		}
	}

	//
	// Payments
	//
	
	public void listPayments(String[] cmds, Map<String, Object> options) {
		if (isHelp2()) {
			return;
		}
		Pageable pageable = convert(options, PageOptions.class).toPageRequest();
		PaymentFilter filter = convert(options, PaymentFilter.class);
		debug("Payment: %s %s", filter, pageable);
		Page<Payment> payments = paymentsClient.listPayments(filter, pageable);
		print(payments);
	}
	
	public void getPayment(String[] cmds, Map<String, Object> options) {
		if (isHelp2()) {
			return;
		}
		String paymentId = argId(op, cmds);
		debug("Payment: %s", paymentId);
		PaymentOptions options_ = convert(options, PaymentOptions.class);
		Payment payment = null; paymentsClient.getPayment(paymentId, options_);
		printObj(payment);
	}
	
	public void submitPayment(String[] cmds, Map<String, Object> options) {
		if (isHelp2()) {
			return;
		}
		Payment payment = convert(options, Payment.class);
		PaymentOptions options_ = convert(options, PaymentOptions.class);
		debug("Creating Payment: %s %s", payment, options_);
		if (isDryrun()) {
			return;
		}
		URI uri = paymentsClient.submitPayment(payment, options_);
		if (isEcho()) {
			debug("Payment URI: %s", uri);
			String id = UriUtils.extractId(uri);
			Payment payment2 = paymentsClient.getPayment(id, null);
			printObj(payment2);			
		}
	}

	public void updatePayment(String[] cmds, Map<String, Object> options) {
		if (isHelp2()) {
			return;
		}
		String paymentId = argId(op, cmds);
		Payment payment = convert(options, Payment.class);
		PaymentOptions options_ = convert(options, PaymentOptions.class);
		debug("Updating Payment: %s %s %s", paymentId, payment, options_);
		if (isDryrun()) {
			return;
		}
		paymentsClient.updatePayment(paymentId, payment, options_);
		if (isEcho()) {
			Payment payment2 = paymentsClient.getPayment(paymentId, null);
			printObj(payment2);			
		}
	}
	
	public void deletePayment(String[] cmds, Map<String, Object> options) {
		if (isHelp2()) {
			return;
		}
		String paymentId = argId(op, cmds);
		debug("Deleting Payment: %s", paymentId);
		if (isDryrun()) {
			return;
		}
		paymentsClient.deletePayment(paymentId, null);	
		if (isEcho()) {
			listPayments(cmds, options);
		}
	}
	
	//
	// Taxs
	//
	
	public void listTaxes(String[] cmds, Map<String, Object> options) {
		if (isHelp2()) {
			return;
		}
		Pageable pageable = convert(options, PageOptions.class).toPageRequest();
		TaxFilter filter = convert(options, TaxFilter.class);
		debug("Taxes: %s %s", filter, pageable);
		Page<Tax> taxs = paymentsClient.listTaxes(filter, pageable);
		print(taxs);
	}
	
	public void getTax(String[] cmds, Map<String, Object> options) {
		if (isHelp2()) {
			return;
		}
		String taxId = argId(op, cmds);
		debug("Tax: %s", taxId);
		TaxOptions options_ = convert(options, TaxOptions.class);
		Tax tax = null; paymentsClient.getTax(taxId, options_);
		printObj(tax);
	}
	
	public void createTax(String[] cmds, Map<String, Object> options) {
		if (isHelp2()) {
			return;
		}
		Tax tax = convert(options, Tax.class);
		TaxOptions options_ = convert(options, TaxOptions.class);
		debug("Creating Tax: %s %s", tax, options_);
		if (isDryrun()) {
			return;
		}
		URI uri = paymentsClient.createTax(tax, options_);
		if (isEcho()) {
			debug("Tax URI: %s", uri);
			String id = UriUtils.extractId(uri);
			Tax tax2 = paymentsClient.getTax(id, null);
			printObj(tax2);			
		}
	}

	public void updateTax(String[] cmds, Map<String, Object> options) {
		if (isHelp2()) {
			return;
		}
		String taxId = argId(op, cmds);
		Tax tax = convert(options, Tax.class);
		TaxOptions options_ = convert(options, TaxOptions.class);
		debug("Updating Tax: %s %s %s", taxId, tax, options_);
		if (isDryrun()) {
			return;
		}
		paymentsClient.updateTax(taxId, tax, options_);
		if (isEcho()) {
			Tax tax2 = paymentsClient.getTax(taxId, null);
			printObj(tax2);			
		}
	}
	
	public void deleteTax(String[] cmds, Map<String, Object> options) {
		if (isHelp2()) {
			return;
		}
		String taxId = argId(op, cmds);
		debug("Deleting Tax: %s", taxId);
		if (isDryrun()) {
			return;
		}
		paymentsClient.deleteTax(taxId, null);
		if (isEcho()) {
			listTaxes(cmds, options);
		}
	}
	
	@Override
	protected String getDefaultFormat(Class<? extends Object> type) {
		if (Account.class.equals(type)) {
			return ACCOUNT_DEFAULT_FORMAT;
		}
		if (Payment.class.equals(type)) {
			return PAYMENT_DEFAULT_FORMAT;
		}
		if (Payable.class.equals(type)) {
			return PAYABLE_DEFAULT_FORMAT;
		}
		if (Card.class.equals(type)) {
			return CARD_DEFAULT_FORMAT;
		}
		if (BankAccount.class.equals(type)) {
			return BANKACCOUNT_DEFAULT_FORMAT;
		}
		if (Tax.class.equals(type)) {
			return TAX_DEFAULT_FORMAT;
		}
		return super.getDefaultFormat(type);
	}

	@Override
	protected String getWideFormat(Class<? extends Object> type) {
		if (Account.class.equals(type)) {
			return ACCOUNT_WIDE_FORMAT;
		}
		if (Payment.class.equals(type)) {
			return PAYMENT_WIDE_FORMAT;
		}
		if (Payable.class.equals(type)) {
			return PAYABLE_WIDE_FORMAT;
		}
		if (Card.class.equals(type)) {
			return CARD_WIDE_FORMAT;
		}
		if (BankAccount.class.equals(type)) {
			return BANKACCOUNT_WIDE_FORMAT;
		}
		if (Tax.class.equals(type)) {
			return TAX_WIDE_FORMAT;
		}
		return super.getWideFormat(type);
	}
	
}