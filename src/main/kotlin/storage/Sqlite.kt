package com.aliadnani.storage

import java.sql.Connection
import java.sql.DriverManager
import java.sql.PreparedStatement
import java.sql.ResultSet

private val MIGRATIONS: List<String> =
    listOf(
        """
        CREATE TABLE IF NOT EXISTS categories (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            name TEXT NOT NULL,
            UNIQUE (name)
        )
        """,
        """
        CREATE TABLE IF NOT EXISTS transactions (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            type TEXT NOT NULL CHECK (type IN ('INCOME', 'EXPENSE')),
            amount TEXT NOT NULL,
            description TEXT NOT NULL,
            transaction_time TEXT NOT NULL,
            category_id INTEGER REFERENCES categories (id)
        )
        """,
    )

class Sqlite(private val path: String = "pft.db") {

  val connection: Connection by lazy {
    DriverManager.getConnection("jdbc:sqlite:$path").also { connection ->
      connection.createStatement().use { it.execute("PRAGMA foreign_keys = ON") }
      migrate(connection)
    }
  }

  private fun migrate(connection: Connection) {
    connection.createStatement().use { stmt ->
      for (sql in MIGRATIONS) {
        stmt.execute(sql)
      }
    }
  }
}

internal inline fun <T> Connection.prepare(sql: String, block: (PreparedStatement) -> T): T =
    prepareStatement(sql).use(block)

internal inline fun <T> Connection.prepareAndBind(
    sql: String,
    bind: (PreparedStatement) -> Unit,
    block: (PreparedStatement) -> T,
): T =
    prepareStatement(sql).use { ps ->
      bind(ps)
      block(ps)
    }

internal inline fun <T> PreparedStatement.mapQueryOne(map: (ResultSet) -> T): T? =
    executeQuery().use { rs -> if (rs.next()) map(rs) else null }

internal inline fun <T> PreparedStatement.mapQueryMany(map: (ResultSet) -> T): List<T> =
    executeQuery().use { rs ->
      buildList {
        while (rs.next()) add(map(rs))
      }
    }
