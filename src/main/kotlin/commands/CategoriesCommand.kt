package com.aliadnani.commands

import com.aliadnani.CommandFailedException
import com.aliadnani.services.CategoriesService
import picocli.CommandLine.Command
import picocli.CommandLine.Model.CommandSpec
import picocli.CommandLine.Option
import picocli.CommandLine.Spec

@Command(name = "categories", description = ["Manage categories"])
class CategoriesCommand(private val categoriesService: CategoriesService) {

  @Spec lateinit var spec: CommandSpec

  private val out
    get() = spec.commandLine().out

  @Command(name = "add", description = ["Add a new category"])
  fun addCategory(
      @Option(names = ["--name"], required = true, paramLabel = "NAME") name: String,
  ) {
    categoriesService.addCategory(name).let { result ->
      when (result) {
        is CategoriesService.CreateCategoryResult.Success -> {
          out.println("Category added: ${result.category}")
        }

        CategoriesService.CreateCategoryResult.AlreadyExists -> {
          throw CommandFailedException("Category already exists: $name")
        }
      }
    }
  }

  @Command(name = "list", description = ["List all categories"])
  fun listCategories() {
    val categories = categoriesService.listCategories()

    out.println(categories)
  }

  @Command(name = "get", description = ["Get a category by name"])
  fun getCategoryByName(
      @Option(names = ["--name"], required = true, paramLabel = "NAME") name: String
  ) {
    val category =
        categoriesService.getCategoryByName(name)
            ?: throw CommandFailedException("Category not found: $name")

    out.println(category)
  }

  @Command(name = "edit", description = ["Edit a category"])
  fun editCategory(
      @Option(names = ["--name"], required = true, paramLabel = "NAME") name: String,
      @Option(names = ["--new-name"], paramLabel = "TEXT") newName: String?,
  ) {
    when (val result = categoriesService.editCategoryKeyedByName(name, newName)) {
      is CategoriesService.EditCategoryResult.Success -> {
        out.println("Category updated: ${result.category}")
      }

      CategoriesService.EditCategoryResult.NotFound -> {
        throw CommandFailedException("Category not found: $name")
      }

      CategoriesService.EditCategoryResult.AlreadyExists -> {
        throw CommandFailedException("Category already exists with the new name: $newName")
      }
    }
  }

  @Command(name = "delete", description = ["Delete a category by name"])
  fun deleteCategoryByName(
      @Option(names = ["--name"], required = true, paramLabel = "NAME") name: String
  ) {
    val result = categoriesService.deleteCategoryByName(name)

    when (result) {
      CategoriesService.DeleteCategoryResult.Success -> {
        out.println("Category deleted: $name")
      }

      CategoriesService.DeleteCategoryResult.NotFound -> {
        throw CommandFailedException("Category not found: $name")
      }
    }
  }
}
