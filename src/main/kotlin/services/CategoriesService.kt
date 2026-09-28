package com.aliadnani.services

import com.aliadnani.model.Category
import com.aliadnani.repos.CategoriesRepo

class CategoriesService(private val categoriesRepo: CategoriesRepo) {
  fun getCategoryByName(name: String): Category? = categoriesRepo.getCategoryByName(name)

  fun listCategories(): List<Category> = categoriesRepo.listCategories()

  sealed interface CreateCategoryResult {
    data class Success(val category: Category) : CreateCategoryResult

    data object AlreadyExists : CreateCategoryResult
  }

  fun addCategory(name: String): CreateCategoryResult {
    if (categoriesRepo.getCategoryByName(name) != null) {
      return CreateCategoryResult.AlreadyExists
    }

    return CreateCategoryResult.Success(categoriesRepo.addCategory(name))
  }

  sealed interface EditCategoryResult {
    data class Success(val category: Category) : EditCategoryResult

    data object NotFound : EditCategoryResult

    data object AlreadyExists : EditCategoryResult
  }

  fun editCategoryKeyedByName(name: String, newName: String?): EditCategoryResult {
    val categoryToBeEdited =
        categoriesRepo.getCategoryByName(name) ?: return EditCategoryResult.NotFound

    if (newName != null && categoriesRepo.getCategoryByName(newName) != null) {
      return EditCategoryResult.AlreadyExists
    }

    return EditCategoryResult.Success(
        categoriesRepo.editCategory(categoryToBeEdited.id, newName ?: name)
    )
  }

  sealed interface DeleteCategoryResult {
    data object Success : DeleteCategoryResult

    data object NotFound : DeleteCategoryResult
  }

  fun deleteCategoryByName(name: String): DeleteCategoryResult {
    return if (categoriesRepo.deleteCategoryByName(name) == 1) {
      DeleteCategoryResult.Success
    } else {
      DeleteCategoryResult.NotFound
    }
  }
}
