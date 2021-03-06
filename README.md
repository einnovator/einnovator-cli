# EInnovator Command-Line Tool (CLI)

This is the repository for **EInnovator** CLI tool. The tool is designed to be extensible and support multiple service as backend.
The overall goal of the tool is to support multi-cluster/multi-cloud Kubernetes devops, and micro-service development. 
Out-of-the-box, it supports integration with **Cloud Manager** and several services in **EInnovator** micro-service suite.

- sso - SSO operations
- devops - Cloud Manager Devops operations
- notifications - Notifications Hub operations
- documents - Document Store operations
- social - Social Hub operations
- payments - Payment Gateway operations

The following help message is displayed by default:

```
Welcome to the EInnovator CLI (Command-Line Tool).
Your super-duper CLI tool for Multi-Cluster/Multi-Cloud Kubernetes devops and micro-services development.

Type a name to get help on commands for that service:

  sso           SSO operations
  devops        Cloud Manager Devops operations
  notifications Notifications Hub operations
  documents     Document Store operations
  social        Social Hub operations
  payments      Payment Gateway operations

Generic commands:

  ls         List Spaces (and other resources)
  pwd        Show current default/parent resources
  cd         Change current Space (and Cluster)
  version    Show CLI version
  set        Set environment variable
  echo       Echo expression list
  exit       Exit interactive mode
  help       Show this help
  login      Login to Server
  api        API operations
  token      OAuth Token operations
  run        Create and run Deployment (or Job or CronJob)
  kill       Stop or delete Deployment (or Job or CronJob)
  market     List Marketplace Solutions
  install    Install Solution (standalone or from Catalog)

Usage: ei sso | devops | notifications | documents | social | payments | 
  ls | pwd | cd | version | set | echo | exit | help args... [-option value]* [--options=value]*
```

## Related Resources

- [**Cloud Manager** CLI Tool Reference](https://cms.einnovator.org/publication/cloud-manager-reference-manual/_/cli.md)

## Command Resolution

Each service is supported by implementing a `CommandRunner`. Commands are resolved by iterating over the collection of registered `CommandRunner`.
Commands can be prefixed by the service name it implements the command for direct resolution, but this is not very convenient for usage.
Some command are general purpose and are implemented by `Generic`. This `CommandRunner` deletes execution of commands to other runners
based on flags on commands. The following commands are considered general purpose:

- ls - List resources (Spaces by default; implemented by Devops)
- kill - Kill workloads (Deployments by default; implemented by Devops)


## Extending the CLI

Each service is supported by implementing a `CommandRunner`, typically by extending `CommandRunnerBase`.
Each runner as a unique name returned by method `CommandRunner.getName()`.
To register a  `CommandRunner` add in in the list returned by method `AppConfig.getAllRunners`.

Method `CommandRunner.init` is called to initialize the runner.
Method `CommandRunner.run` is called to run a command.
Parameters `type` and `op`, represent the first and second arguments in the command-line. The remaining arcuments are passed in parameter `args`.
Options are passed in parameter `options`. 

The easiest way to create a `CommandRunner` is by using an existing one as example.
`Social` is a good one to take a look to grab the basic structure and code patterns to do CRUD operations in entities managed by another service.
Each of the provided `CommandRunner` uses a corresponding client library to connect to a backend service.
The `SSO` runner the special purpose task of implementing OAuth2 token based login. 
It also supports a single-user mode with BASIC authentication, for services deployed in a mode that does not integrated with the SSO Gateway.
For example, **Cloud Manager** can be run in multi-user or single-user mode. The tool used the `/info` endpoint of the service to detected which mode to use.

Method `CommandRunner.getCommands` is called to return the list of top-level commands returned by the runner.
Each command can have a list of aliases. Method `CommandRunner.getSubCommands` is used to return sub-commands, and 
method `CommandRunner.getSubSubCommands` is used to return sub-sub-commands.
	
Providing proper help messages is a big part of implementing a runner. File `message.proprerties` contains the messages for all the commands.
To create help for a command use the following keys:

```
service.command = One-line description
service.command.args = Comma-separated list of arguments (+ suffix implies optional; ++ implies varied number)
service.command.args.name = One-line description of argument
service.command.options = Comma-separated list of options (+ suffix implies optional; ++ implies varied number). For alias, use: name1|name2|..
service.command.options.name = One-line description of options (name only first alias)
service.command.descr = Multi-line description
```

To show help call methods `CommandRunnerBase.printUsage*()` or `CommandRunnerBase.isHelp*()`.

