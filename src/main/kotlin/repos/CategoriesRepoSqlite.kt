package com.aliadnani.repos

import com.aliadnani.model.Category
import com.aliadnani.storage.Sqlite
import com.aliadnani.storage.mapQueryMany
import com.aliadnani.storage.mapQueryOne
import com.aliadnani.storage.prepare
import com.aliadnani.storage.prepareAndBind
import java.sql.ResultSet

private fun fromRow(rs: ResultSet): Category =
    Category(id = rs.getInt("id"), name = rs.getString("name"))

class CategoriesRepoSqlite(private val db: Sqlite) : CategoriesRepo {

  override fun getCategoryByName(name: String): Category? =
      db.connection.prepareAndBind(
          "SELECT id, name FROM categories WHERE name = ?",
          { it.setString(1, name) },
          { it.mapQueryOne(::fromRow) },
      )

  override fun listCategories(): List<Category> =
      db.connection.prepare("SELECT id, name FROM categories ORDER BY name") {
        it.mapQueryMany(::fromRow)
      }

  override fun addCategory(name: String): Category =
      db.connection.prepareAndBind(
          "INSERT INTO categories (name) VALUES (?)",
          { it.setString(1, name) },
      ) { ps ->
        ps.executeUpdate()
        val id =
            ps.generatedKeys.use { keys ->
              check(keys.next()) { "Expected generated key after inserting category" }
              keys.getInt(1)
            }
        Category(id = id, name = name)
      }

  override fun editCategory(id: Int, newName: String): Category =
      db.connection.prepareAndBind(
          "UPDATE categories SET name = ? WHERE id = ?",
          {
            it.setString(1, newName)
            it.setInt(2, id)
          },
      ) { ps ->
        ps.executeUpdate()
        Category(id = id, name = newName)
      }

  override fun deleteCategoryByName(name: String): Int {
    val connection = db.connection
    val previousAutoCommit = connection.autoCommit
    connection.autoCommit = false
    try {
      val categoryId =
          connection.prepareAndBind(
              "SELECT id FROM categories WHERE name = ?",
              { it.setString(1, name) },
              { it.mapQueryOne { rs -> rs.getInt("id") } },
          )
              ?: run {
                connection.commit()
                return 0
              }

      connection.prepareAndBind(
          "UPDATE transactions SET category_id = NULL WHERE category_id = ?",
          { it.setInt(1, categoryId) },
          { it.executeUpdate() },
      )

      val deletedRows =
          connection.prepareAndBind(
              "DELETE FROM categories WHERE id = ?",
              { it.setInt(1, categoryId) },
              { it.executeUpdate() },
          )

      connection.commit()
      return deletedRows
    } catch (exception: Exception) {
      connection.rollback()
      throw exception
    } finally {
      connection.autoCommit = previousAutoCommit
    }
  }
}
