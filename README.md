## Gerrit Slack Bot

This is a program that monitors a [Gerrit](https://www.gerritcodereview.com/) 
[event stream](https://gerrit-review.googlesource.com/Documentation/cmd-stream-events.html)
and sends notifications to Slack channels and users via direct messages based on a provided configuration file.
It is written in Kotlin and requires a JVM to run.

### Building

This project uses the gradle application plugin to build a distribution zip/tar. To build the distribution files, invoke 
gradle as follows:

`./gradlew clean app:assembleDist`

A ZIP and TAR file will be created in `app/build/distributions/`

Unfortunately it is [not possible](https://github.com/johnrengelman/shadow/issues/227) to distribute the program as a 
single "fat" or "uber" jar, due to the necessity to bundle BouncyCastle, a cryptography provider, and the requirement 
that such providers be signed. The process of building the fat jar destroys the signature.

### Running the program

Unzip/tar the distribution, and from the directory created, run the program as follows:

`./gerrit-slack-bot -c <path-to-config.yaml>`

### Config file

A configuration file is required for the program to do anything useful. The distribution contains an [example
config](app/src/dist/config.yaml) with comments.
