package com.aliadnani

import kotlin.system.exitProcess

fun main(args: Array<String>): Unit = exitProcess(buildPftCommandLine().execute(*args))
