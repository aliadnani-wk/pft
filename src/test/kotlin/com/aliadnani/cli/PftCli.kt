package com.aliadnani.cli

import com.aliadnani.buildPftCommandLine
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.nio.file.Files

class PftCli {
  val workingDir: File = Files.createTempDirectory("pft-test").toFile()

  private val dbFile: File = File(workingDir, "pft.db")

  fun writeFile(name: String, content: String): File =
      File(workingDir, name).apply { writeText(content) }

  fun run(vararg args: String): Result {
    val writer = StringWriter()
    val commandLine = buildPftCommandLine(dbFile.absolutePath)
    commandLine.setOut(PrintWriter(writer))
    commandLine.setErr(PrintWriter(writer))

    val exitCode = commandLine.execute(*args)

    return Result(exitCode, writer.toString().trim())
  }

  fun cleanup() {
    workingDir.deleteRecursively()
  }

  data class Result(val exitCode: Int, val output: String)
}
