package com.aliadnani.repos

import com.aliadnani.model.Category
import com.aliadnani.model.NewTransaction
import com.aliadnani.model.Transaction
import com.aliadnani.storage.Sqlite
import com.aliadnani.storage.mapQueryMany
import com.aliadnani.storage.mapQueryOne
import com.aliadnani.storage.prepare
import com.aliadnani.storage.prepareAndBind
import java.math.BigDecimal
import java.sql.ResultSet
import java.sql.Types
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

private val TIME_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE_TIME

private fun fromRow(rs: ResultSet): Transaction {
  val categoryId = rs.getInt("c_id")
  val category =
      if (rs.wasNull()) null else Category(id = categoryId, name = rs.getString("c_name"))

  return Transaction(
      id = rs.getInt("id"),
      transactionType = Transaction.TransactionType.valueOf(rs.getString("type")),
      amount = BigDecimal(rs.getString("amount")),
      description = rs.getString("description"),
      timestamp = LocalDateTime.parse(rs.getString("transaction_time"), TIME_FORMAT),
      category = category,
  )
}

private val INSERT_TRANSACTION_SQL =
    """
    INSERT INTO transactions (type, amount, description, transaction_time, category_id)
    VALUES (?, ?, ?, ?, ?)
    """
        .trimIndent()

class TransactionsRepoSqlite(private val db: Sqlite) : TransactionsRepo {

  override fun getTransactionById(id: Int): Transaction? =
      db.connection.prepareAndBind(
          """
          SELECT t.*, c.id AS c_id, c.name AS c_name
          FROM transactions t
          LEFT JOIN categories c ON c.id = t.category_id
          WHERE t.id = ?
          """
              .trimIndent(),
          { it.setInt(1, id) },
          { ps -> ps.mapQueryOne(::fromRow) },
      )

  override fun listTransactions(
      before: LocalDateTime?,
      after: LocalDateTime?,
      categoryNames: List<String>?,
  ): List<Transaction> {
    val conditions = mutableListOf<String>()
    val binds = mutableListOf<String>()

    before?.let {
      conditions += "t.transaction_time < ?"
      binds += it.toString()
    }
    after?.let {
      conditions += "t.transaction_time > ?"
      binds += it.toString()
    }
    if (!categoryNames.isNullOrEmpty()) {
      conditions += "c.name IN (${categoryNames.joinToString(", ") { "?" }})"
      binds += categoryNames
    }

    val whereClause =
        conditions.takeIf { it.isNotEmpty() }?.joinToString(" AND ", prefix = "WHERE ") ?: ""

    val sql =
        """
        SELECT t.*, c.id AS c_id, c.name AS c_name
        FROM transactions t
        LEFT JOIN categories c ON c.id = t.category_id
        $whereClause
        ORDER BY t.transaction_time DESC, t.id DESC
        """
            .trimIndent()

    return db.connection.prepareAndBind(
        sql,
        { ps -> binds.forEachIndexed { index, value -> ps.setString(index + 1, value) } },
        { ps -> ps.mapQueryMany(::fromRow) },
    )
  }

  override fun findTransactionsMadeOnWeekends(): List<Transaction> =
      db.connection.prepare(
          """
          SELECT t.*, c.id AS c_id, c.name AS c_name
          FROM transactions t
          LEFT JOIN categories c ON c.id = t.category_id
          WHERE CAST(strftime('%w', t.transaction_time) AS INTEGER) IN (0, 6)
          ORDER BY t.transaction_time DESC, t.id DESC
          """
              .trimIndent(),
      ) { ps ->
        ps.mapQueryMany(::fromRow)
      }

  override fun addTransaction(
      transactionType: Transaction.TransactionType,
      amount: BigDecimal,
      description: String,
      transactionTime: LocalDateTime,
      categoryId: Int?,
  ): Int =
      db.connection.prepareAndBind(
          INSERT_TRANSACTION_SQL,
          {
            it.setString(1, transactionType.name)
            it.setString(2, amount.toPlainString())
            it.setString(3, description)
            it.setString(4, transactionTime.toString())
            categoryId?.let { id -> it.setInt(5, id) } ?: it.setNull(5, Types.INTEGER)
          },
      ) { ps ->
        ps.executeUpdate()
        ps.generatedKeys.use { keys ->
          check(keys.next()) { "Expected generated key after inserting transaction" }
          keys.getInt(1)
        }
      }

  override fun addTransactions(transactions: List<NewTransaction>) {
    if (transactions.isEmpty()) {
      return
    }

    val connection = db.connection
    val previousAutoCommit = connection.autoCommit
    connection.autoCommit = false
    try {
      connection.prepareStatement(INSERT_TRANSACTION_SQL).use { ps ->
        for (transaction in transactions) {
          ps.setString(1, transaction.transactionType.name)
          ps.setString(2, transaction.amount.toPlainString())
          ps.setString(3, transaction.description)
          ps.setString(4, transaction.timestamp.toString())
          transaction.categoryId?.let { id -> ps.setInt(5, id) } ?: ps.setNull(5, Types.INTEGER)
          ps.addBatch()
        }
        ps.executeBatch()
      }
      connection.commit()
    } catch (exception: Exception) {
      connection.rollback()
      throw exception
    } finally {
      connection.autoCommit = previousAutoCommit
    }
  }

  override fun editTransaction(
      id: Int,
      transactionType: Transaction.TransactionType,
      amount: BigDecimal,
      description: String,
      transactionTime: LocalDateTime,
      categoryId: Int?,
  ) {
    db.connection.prepareAndBind(
        """
        UPDATE transactions
        SET type = ?, amount = ?, description = ?, transaction_time = ?, category_id = ?
        WHERE id = ?
        """
            .trimIndent(),
        {
          it.setString(1, transactionType.name)
          it.setString(2, amount.toPlainString())
          it.setString(3, description)
          it.setString(4, transactionTime.toString())
          categoryId?.let { cId -> it.setInt(5, cId) } ?: it.setNull(5, Types.INTEGER)
          it.setInt(6, id)
        },
    ) { ps ->
      ps.executeUpdate()
    }
  }

  override fun deleteTransaction(id: Int): Int =
      db.connection.prepareAndBind(
          "DELETE FROM transactions WHERE id = ?",
          { it.setInt(1, id) },
          { ps -> ps.executeUpdate() },
      )
}
