package com.aliadnani.repos

import com.aliadnani.model.Category

interface CategoriesRepo {
  fun getCategoryByName(name: String): Category?

  fun listCategories(): List<Category>

  fun addCategory(name: String): Category

  fun editCategory(id: Int, newName: String): Category

  fun deleteCategoryByName(name: String): Int
}
