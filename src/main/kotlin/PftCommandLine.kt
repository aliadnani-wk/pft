package com.aliadnani

import picocli.CommandLine

class CommandFailedException(message: String) : RuntimeException(message)

fun buildPftCommandLine(dbPath: String = "pft.db"): CommandLine =
    CommandLine(Pft(), Pft.Factory(dbPath))
        .setCaseInsensitiveEnumValuesAllowed(true)
        .setExecutionExceptionHandler { exception, commandLine, _ ->
          if (exception is CommandFailedException) {
            commandLine.err.println(exception.message)
            1
          } else {
            exception.printStackTrace(commandLine.err)
            commandLine.commandSpec.exitCodeOnExecutionException()
          }
        }
