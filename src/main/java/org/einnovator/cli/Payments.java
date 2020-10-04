package org.einnovator.cli;

import static  org.einnovator.util.MappingUtils.convert;
import static org.einnovator.util.MappingUtils.updateObjectFromNonNull;

import java.net.URI;
import java.util.Map;

import org.einnovator.payments.client.PaymentsClient;
import org.einnovator.payments.client.config.PaymentsClientConfiguration;
import org.einnovator.payments.client.model.Account;
import org.einnovator.payments.client.model.Payment;
import org.einnovator.payments.client.modelx.AccountFilter;
import org.einnovator.payments.client.modelx.AccountOptions;
import org.einnovator.payments.client.modelx.PaymentFilter;
import org.einnovator.payments.client.modelx.PaymentOptions;
import org.einnovator.util.PageOptions;
import org.einnovator.util.UriUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.oauth2.client.DefaultOAuth2ClientContext;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.security.oauth2.client.token.grant.password.ResourceOwnerPasswordResourceDetails;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.stereotype.Component;


@Component
public class Payments extends CommandRunnerBase {
	private static final String PAYMENTS_DEFAULT_SERVER = "http://localhost:2061";

	@Autowired
	Sso sso;

	OAuth2AccessToken token;
	
	private PaymentsClient paymentsClient;

	private PaymentsClientConfiguration config = new PaymentsClientConfiguration();

	
	
	@Override
	public String getPrefix() {
		return "payments";
	}

	String[] PAYMENTS_COMMANDS = new String[] { 
		"accounts", "account", "acc",
		"payments", "payment", "notific", "n",
		};

	protected String[] getCommands() {
		return PAYMENTS_COMMANDS;
	}



	public void init(Map<String, Object> args) {
		super.init(args, template);
		config.setServer(PAYMENTS_DEFAULT_SERVER);
		updateObjectFromNonNull(config, convert(args, PaymentsClientConfiguration.class));

		ResourceOwnerPasswordResourceDetails resource = sso.getRequiredResourceDetails();
		DefaultOAuth2ClientContext context = new DefaultOAuth2ClientContext();
		OAuth2RestTemplate template = new OAuth2RestTemplate(resource, context);
		template.setRequestFactory(config.getConnection().makeClientHttpRequestFactory());
		
		paymentsClient = new PaymentsClient(template, config);
	}

	public void run(String type, String op, Map<String, Object> argsMap, String[] args) {

		switch (type) {
		case "accounts": case "account": case "acc":
			switch (op) {
			case "get": case "g": case "show": case "s": case "view": case "v":
				getAccount(argsMap);
				break;
			case "list": case "l": case "":
				listAccounts(argsMap);
				break;
			case "delete": case "del": case "d":
				deleteAccount(argsMap);
				break;
			default: 
				invalidOp(type, op);
				break;
			}
			break;

		case "payments": case "payment": case "pay":
			switch (op) {
			case "get": case "g": case "show": case "s": case "view": case "v":
				getPayment(argsMap);
				break;
			case "list": case "l": case "":
				listPayments(argsMap);
				break;
			case "delete": case "del": case "d":
				deletePayment(argsMap);
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
	
	public void listAccounts(Map<String, Object> args) {
		Pageable pageable = convert(args, PageOptions.class).toPageRequest();
		AccountFilter filter = convert(args, AccountFilter.class);
		Page<Account> accounts = paymentsClient.listAccounts(filter, pageable);
		printLine("Listing Accounts...");
		printLine("Filter:", filter);
		printLine("Pageable:", pageable);
		printLine("Accounts:");
		print(accounts);
	}
	
	public void getAccount(Map<String, Object> args) {
		String accountId = (String)get(new String[] {"id", "uuid"}, args);
		AccountOptions options = convert(args, AccountOptions.class);
		Account account = null; paymentsClient.getAccount(accountId, options);
		printLine("Get Account...");
		printLine("ID:", accountId);
		printLine("Account:");
		print(account);
	}
	
	public void createAccount(Map<String, Object> args) {
		Account account = convert(args, Account.class);
		printLine("Creating Account...");
		print(account);
		URI uri = paymentsClient.createAccount(account, null);
		printLine("URI:", uri);
		String accountId = UriUtils.extractId(uri);
		Account account2 = null; paymentsClient.getAccount(accountId, null);
		print("Created Account:");
		print(account2);
	}

	public void updateAccount(Map<String, Object> args) {
		String accountId = (String)get(new String[] {"id", "uuid"}, args);
		Account account = convert(args, Account.class);
		printLine("Updating Account...");
		print(account);
		paymentsClient.updateAccount(account, null);
		Account account2 = null; paymentsClient.getAccount(accountId, null);
		print("Updated Account:");
		print(account2);

	}
	
	public void deleteAccount(Map<String, Object> args) {
		String accountId = (String)get(new String[] {"id", "uuid"}, args);
		printLine("Deleting Account...");
		printLine("ID:", accountId);		
		paymentsClient.deleteAccount(accountId, null);		
	}

	//
	// Payments
	//
	
	public void listPayments(Map<String, Object> args) {
		Pageable pageable = convert(args, PageOptions.class).toPageRequest();
		PaymentFilter filter = convert(args, PaymentFilter.class);
		Page<Payment> payments = paymentsClient.listPayments(filter, pageable);
		printLine("Listing Payments...");
		printLine("Filter:", filter);
		printLine("Pageable:", pageable);
		printLine("Payments:");
		print(payments);
	}
	
	public void getPayment(Map<String, Object> args) {
		String paymentId = (String)get(new String[] {"id", "uuid"}, args);
		PaymentOptions options = convert(args, PaymentOptions.class);
		Payment payment = null; paymentsClient.getPayment(paymentId, options);
		printLine("Get Payment...");
		printLine("ID:", paymentId);
		printLine("Payment:");
		print(payment);
	}
	
	public void createPayment(Map<String, Object> args) {
		Payment payment = convert(args, Payment.class);
		printLine("Creating Payment...");
		print(payment);
		URI uri = paymentsClient.submitPayment(payment, null);
		printLine("URI:", uri);
		String paymentId = UriUtils.extractId(uri);
		Payment payment2 = null; paymentsClient.getPayment(paymentId, null);
		print("Created Payment:");
		print(payment2);
	}

	public void updatePayment(Map<String, Object> args) {
		String paymentId = (String)get(new String[] {"id", "uuid"}, args);
		Payment payment = convert(args, Payment.class);
		printLine("Updating Payment...");
		print(payment);
		paymentsClient.updatePayment(payment, null);
		Payment payment2 = null; paymentsClient.getPayment(paymentId, null);
		print("Updated Payment:");
		print(payment2);

	}
	
	public void deletePayment(Map<String, Object> args) {
		String paymentId = (String)get(new String[] {"id", "uuid"}, args);
		printLine("Deleting Payment...");
		printLine("ID:", paymentId);		
		paymentsClient.deletePayment(paymentId, null);		
	}
	
	
	
}