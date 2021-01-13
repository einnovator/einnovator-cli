# EInnovator Command-Line Tool (CLI)

This is the repository for **EInnovator** CLI tool. The tools is designed to be extensible and support multiple service as backend.
The overall goal of the tool is to support multi-cluster/multi-cloud Kubernetes devops, and micro-service development. 
Out-of-the-box, it supports integration with **Cloud Manager** and several services in **EInnovator** micro-service suite.

- sso  ---          SSO operations
- devops ---    Cloud Manager Devops operations
- notifications --- Notifications Hub operations
- documents  ---   Document Store operations
- social     ---   Social Hub operations
- payments   ---    Payment Gateway operations

## Command Resolution

Each service is supported by implementing a `CommandRunner`. Commands are resolved by iterating over the collection of registered `CommandRunner`.
Commands can be prefixed by the service name it implements the command for direct resolution, but this is not very convenient for usage.
Some command are general purpose and are implemented by `Generic`. This `CommandRunner` deletes execution of commands to other runners
based on flags on commands. The following commands are considered general purpose:

-  ls   --- List resources (Spaces by default; implemented by Devops)
-  kill   --- Kill workloads (Deployments by default; implemented by Devops)


## Extending the CLI

Each service is supported by implementing a `CommandRunner`, typically by extending `CommandRunnerBase`.
Each service as name returned by method `CommandRunner.getName()`.
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

Providing proper help messages is a big part of implementing a runner. File `message.proprerties`  contains the messages for all the commands.
To create help for a command use the following keys:

```
service.command = One line description
service.command.args = Comma-separated list of arguments (+ suffix implies optional; ++ implies varied number)
service.command.args.name = One-line description of argument
service.command.options = Comma-separated list of options (+ suffix implies optional; ++ implies varied number). For alias, use: name1|name2|..
service.command.options.name = One-line description of options (name only first alias)
```

To show help call methods  `CommandRunnerBase.printUsage*()` or `CommandRunnerBase.isHelp*()`.

