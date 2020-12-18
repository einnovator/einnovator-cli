package org.einnovator.cli;

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
import org.einnovator.util.PageOptions;
import org.einnovator.util.UriUtils;
import org.einnovator.util.web.RequestOptions;
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



	public void init(String[] cmds, Map<String, Object> options, OAuth2RestTemplate template, boolean interactive) {
		if (!init) {
			super.init(cmds, options, template, interactive);
			config.setServer(PAYMENTS_DEFAULT_SERVER);
			updateObjectFromNonNull(config, convert(options, PaymentsClientConfiguration.class));

			template = makeOAuth2RestTemplate(sso.getRequiredResourceDetails(), config.getConnection());
			super.init(cmds, options, template, interactive);

			paymentsClient = new PaymentsClient(template, config);
			init = true;
		}
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
			case "delete": case "del": case "rm": case "d":
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
			case "delete": case "del": case "rm": case "d":
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
		debug("Account: %s %s", filter, pageable);
		Page<Account> accounts = paymentsClient.listAccounts(filter, pageable);
		print(accounts);
	}
	
	public void getAccount(String type, String op, String[] cmds, Map<String, Object> options) {
		String accountId = argId(op, cmds);
		debug("Account: %s", accountId);
		AccountOptions options_ = convert(options, AccountOptions.class);
		Account account = paymentsClient.getAccount(accountId, options_);
		printObj(account);
	}
	
	public void createAccount(String type, String op, String[] cmds, Map<String, Object> options) {
		Account account = convert(options, Account.class);
		account.setName(argName(op, cmds));
		debug("Creating Account: %s", account);
		URI uri = paymentsClient.createAccount(account, new RequestOptions());
		if (isEcho()) {
			printLine("Account URI:", uri);
			String id = UriUtils.extractId(uri);
			Account account2 = paymentsClient.getAccount(id, null);
			printObj(account2);			
		}
	}

	public void updateAccount(String type, String op, String[] cmds, Map<String, Object> options) {
		String accountId = argId(op, cmds);
		Account account = convert(options, Account.class);
		debug("Updating Account: %s %s", accountId, account);
		paymentsClient.updateAccount(account, null);
		if (isEcho()) {
			Account account2 = paymentsClient.getAccount(accountId, null);
			printObj(account2);			
		}
	}
	
	public void deleteAccount(String type, String op, String[] cmds, Map<String, Object> options) {
		String accountId = argId(op, cmds);
		debug("Deleting Account: %s", accountId);
		paymentsClient.deleteAccount(accountId, null);		
	}

	//
	// Payments
	//
	
	public void listPayments(String type, String op, String[] cmds, Map<String, Object> options) {
		Pageable pageable = convert(options, PageOptions.class).toPageRequest();
		PaymentFilter filter = convert(options, PaymentFilter.class);
		debug("Payment: %s %s", filter, pageable);
		Page<Payment> payments = paymentsClient.listPayments(filter, pageable);
		print(payments);
	}
	
	public void getPayment(String type, String op, String[] cmds, Map<String, Object> options) {
		String paymentId = argId(op, cmds);
		debug("Payment: %s", paymentId);
		PaymentOptions options_ = convert(options, PaymentOptions.class);
		Payment payment = null; paymentsClient.getPayment(paymentId, options_);
		printObj(payment);
	}
	
	public void submitPayment(String type, String op, String[] cmds, Map<String, Object> options) {
		Payment payment = convert(options, Payment.class);
		debug("Creating Payment: %s", payment);
		URI uri = paymentsClient.submitPayment(payment, new RequestOptions());
		if (isEcho()) {
			printLine("Payment URI:", uri);
			String id = UriUtils.extractId(uri);
			Payment payment2 = paymentsClient.getPayment(id, null);
			printObj(payment2);			
		}
	}

	public void updatePayment(String type, String op, String[] cmds, Map<String, Object> options) {
		String paymentId = argId(op, cmds);
		Payment payment = convert(options, Payment.class);
		debug("Updating Payment: %s %s", paymentId, payment);
		paymentsClient.updatePayment(payment, null);
		if (isEcho()) {
			Payment payment2 = paymentsClient.getPayment(paymentId, null);
			printObj(payment2);			
		}
	}
	
	public void deletePayment(String type, String op, String[] cmds, Map<String, Object> options) {
		String paymentId = argId(op, cmds);
		debug("Deleting Payment: %s", paymentId);
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