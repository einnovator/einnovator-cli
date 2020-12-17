package org.einnovator.cli;

import static  org.einnovator.util.MappingUtils.convert;
import static org.einnovator.util.MappingUtils.updateObjectFromNonNull;

import java.net.URI;
import java.util.Map;

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
import org.einnovator.sso.client.model.Client;
import org.einnovator.util.PageOptions;
import org.einnovator.util.UriUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.stereotype.Component;


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


	@Autowired
	Sso sso;

	OAuth2AccessToken token;
	
	private PaymentsClient paymentsClient;

	private String server = PAYMENTS_DEFAULT_SERVER;

	private PaymentsClientConfiguration config = new PaymentsClientConfiguration();

	
	
	@Override
	public String getPrefix() {
		return "payments";
	}

	String[] PAYMENTS_COMMANDS = new String[] { 
		"accounts", "account", "acc",
		"payments", "payment", "pay"
		};

	protected String[] getCommands() {
		return PAYMENTS_COMMANDS;
	}



	public void init(String[] cmds, Map<String, Object> options, OAuth2RestTemplate template) {
		config.setServer(PAYMENTS_DEFAULT_SERVER);
		updateObjectFromNonNull(config, convert(options, PaymentsClientConfiguration.class));

		template = makeOAuth2RestTemplate(sso.getRequiredResourceDetails(), config.getConnection());
		super.init(cmds, options, template);

		paymentsClient = new PaymentsClient(template, config);
	}

	public void setEndpoints(Map<String, Object> endpoints) {
		String server = (String)endpoints.get("server");
		if (server!=null) {
			this.server = server;
		}
	}

	public void run(String type, String op, String[] cmds, Map<String, Object> options) {

		switch (type) {
		case "accounts": case "account": case "acc":
			switch (op) {
			case "get": case "g": case "show": case "s": case "view": case "v":
				getAccount(type, op, cmds, options);
				break;
			case "list": case "l": case "":
				listAccounts(type, op, cmds, options);
				break;
			case "delete": case "del": case "d":
				deleteAccount(type, op, cmds, options);
				break;
			default: 
				invalidOp(type, op);
				break;
			}
			break;
		case "payments": case "payment": case "pay":
			switch (op) {
			case "get": case "g": case "show": case "s": case "view": case "v":
				getPayment(type, op, cmds, options);
				break;
			case "list": case "l": case "":
				listPayments(type, op, cmds, options);
				break;
			case "delete": case "del": case "d":
				deletePayment(type, op, cmds, options);
				break;
			default: 
				invalidOp(type, op);
				break;
			}
			break;
		default:
			System.err.println("Invalid command: " + type + " " + op);
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
	
	public void listAccounts(String type, String op, String[] cmds, Map<String, Object> options) {
		Pageable pageable = convert(options, PageOptions.class).toPageRequest();
		AccountFilter filter = convert(options, AccountFilter.class);
		Page<Account> accounts = paymentsClient.listAccounts(filter, pageable);
		printLine("Listing Accounts...");
		printLine("Filter:", filter);
		printLine("Pageable:", pageable);
		printLine("Accounts:");
		print(accounts);
	}
	
	public void getAccount(String type, String op, String[] cmds, Map<String, Object> options) {
		String accountId = (String)get(new String[] {"id", "uuid"}, options);
		AccountOptions options_ = convert(options, AccountOptions.class);
		Account account = null; paymentsClient.getAccount(accountId, options_);
		printLine("Get Account...");
		printLine("ID:", accountId);
		printLine("Account:");
		print(account);
	}
	
	public void createAccount(String type, String op, String[] cmds, Map<String, Object> options) {
		Account account = convert(options, Account.class);
		printLine("Creating Account...");
		print(account);
		URI uri = paymentsClient.createAccount(account, null);
		printLine("URI:", uri);
		String accountId = UriUtils.extractId(uri);
		Account account2 = null; paymentsClient.getAccount(accountId, null);
		print("Created Account:");
		print(account2);
	}

	public void updateAccount(String type, String op, String[] cmds, Map<String, Object> options) {
		String accountId = (String)get(new String[] {"id", "uuid"}, options);
		Account account = convert(options, Account.class);
		printLine("Updating Account...");
		print(account);
		paymentsClient.updateAccount(account, null);
		Account account2 = null; paymentsClient.getAccount(accountId, null);
		print("Updated Account:");
		print(account2);

	}
	
	public void deleteAccount(String type, String op, String[] cmds, Map<String, Object> options) {
		String accountId = (String)get(new String[] {"id", "uuid"}, options);
		printLine("Deleting Account...");
		printLine("ID:", accountId);		
		paymentsClient.deleteAccount(accountId, null);		
	}

	//
	// Payments
	//
	
	public void listPayments(String type, String op, String[] cmds, Map<String, Object> options) {
		Pageable pageable = convert(options, PageOptions.class).toPageRequest();
		PaymentFilter filter = convert(options, PaymentFilter.class);
		Page<Payment> payments = paymentsClient.listPayments(filter, pageable);
		printLine("Listing Payments...");
		printLine("Filter:", filter);
		printLine("Pageable:", pageable);
		printLine("Payments:");
		print(payments);
	}
	
	public void getPayment(String type, String op, String[] cmds, Map<String, Object> options) {
		String paymentId = (String)get(new String[] {"id", "uuid"}, options);
		PaymentOptions options_ = convert(options, PaymentOptions.class);
		Payment payment = null; paymentsClient.getPayment(paymentId, options_);
		printLine("Get Payment...");
		printLine("ID:", paymentId);
		printLine("Payment:");
		print(payment);
	}
	
	public void createPayment(String type, String op, String[] cmds, Map<String, Object> options) {
		Payment payment = convert(options, Payment.class);
		printLine("Creating Payment...");
		print(payment);
		URI uri = paymentsClient.submitPayment(payment, null);
		printLine("URI:", uri);
		String paymentId = UriUtils.extractId(uri);
		Payment payment2 = null; paymentsClient.getPayment(paymentId, null);
		print("Created Payment:");
		print(payment2);
	}

	public void updatePayment(String type, String op, String[] cmds, Map<String, Object> options) {
		String paymentId = (String)get(new String[] {"id", "uuid"}, options);
		Payment payment = convert(options, Payment.class);
		printLine("Updating Payment...");
		print(payment);
		paymentsClient.updatePayment(payment, null);
		Payment payment2 = null; paymentsClient.getPayment(paymentId, null);
		print("Updated Payment:");
		print(payment2);

	}
	
	public void deletePayment(String type, String op, String[] cmds, Map<String, Object> options) {
		String paymentId = (String)get(new String[] {"id", "uuid"}, options);
		printLine("Deleting Payment...");
		printLine("ID:", paymentId);		
		paymentsClient.deletePayment(paymentId, null);		
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
		return null;
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
		return null;
	}
	
}