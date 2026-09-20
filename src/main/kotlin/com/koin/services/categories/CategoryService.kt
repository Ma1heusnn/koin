package com.koin.services.categories

import com.koin.factory.DatabaseFactory
import com.koin.models.Category
import com.koin.models.CategoryDTO
import com.koin.models.CategoryPatch
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.or
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update
import com.koin.tables.categories.CategoriesTable
import com.koin.tables.costs.CostsTable
import io.ktor.server.plugins.BadRequestException
import org.jetbrains.exposed.sql.SqlExpressionBuilder.isNull

class CategoryService {

    suspend fun createUserCategory(userId: Int, category: CategoryDTO): Category = DatabaseFactory.dbQuery {

        val generatedId = CategoriesTable.insertAndGetId { statement ->
            statement[name] = category.name
            statement[icon] = category.icon
            statement[color] = category.color
            statement[CategoriesTable.userId] = userId
        }
        Category(
            id = generatedId.value, name = category.name, icon = category.icon, color = category.color, userId = userId
        )
    }

    suspend fun sendGlobalCategories() = DatabaseFactory.dbQuery {

        val globalCategoriesExist = CategoriesTable.selectAll().where { CategoriesTable.userId.isNull() }.empty().not()

        if (globalCategoriesExist) {
            return@dbQuery
        }
        val defaultCategories = listOf(
            CategoryDTO("Saúde e Bem Estar", icon = "health", color = "#00FF00"),
            CategoryDTO("Alimentação", icon = "food", color = "#FFCF61"),
            CategoryDTO("Lazer", icon = "leisure", color = "#82C8FF"),
            CategoryDTO("Transporte", icon = "transport", color = "#CA86FF"),
            CategoryDTO("Educação", icon = "education", color = "#306DFF"),
            CategoryDTO("Investimento", icon = "investments", color = "#FFCF26"),
            CategoryDTO("Sem Categoria", icon = "none", color = "#C2C2C2")
        )


        defaultCategories.forEach { category ->
            CategoriesTable.insert {
                it[name] = category.name
                it[icon] = category.icon
                it[color] = category.color
                it[userId] = null
            }
        }


    }

    suspend fun getCategories(userId: Int): List<Category> = DatabaseFactory.dbQuery {
        CategoriesTable.selectAll().where {
                (CategoriesTable.userId.isNull()) or (CategoriesTable.userId eq userId)
            }.map {
                // M9: userId incluído. Antes ficava de fora e o cliente recebia `userId: null` em
                // TODA categoria da lista — indistinguível de uma categoria global (que é null de
                // verdade). O mesmo recurso vinha com shape diferente conforme a rota
                // posicional aqui é convite a trocar `image` com `color` sem o compilador reclamar.
                Category(
                    id = it[CategoriesTable.id].value,
                    name = it[CategoriesTable.name],
                    icon = it[CategoriesTable.icon],
                    color = it[CategoriesTable.color],
                    userId = it[CategoriesTable.userId]
                )
            }
    }

    suspend fun editCategory(id: Int, userId: Int, patch: CategoryPatch): Boolean = DatabaseFactory.dbQuery {
        CategoriesTable.update(where = { (CategoriesTable.id eq id) and (CategoriesTable.userId eq userId) }) {
            patch.name?.let { newName -> it[name] = newName }
            patch.color?.let { newColor -> it[color] = newColor }
            patch.icon?.let { newIcon -> it[icon] = newIcon }
        } > 0
    }

    suspend fun deleteCategoryById(id: Int, userId: Int, moveToId: Int?): Boolean = DatabaseFactory.dbQuery {


        val deletedCategoryExists =
            CategoriesTable.selectAll().where { (CategoriesTable.id eq id) and (CategoriesTable.userId eq userId) }
                .empty().not()

        if (!deletedCategoryExists) {
            return@dbQuery false
        }
        val costsFromDeletedCategoryExists =
            CostsTable.selectAll().where { (CostsTable.categoryId eq id) and (CostsTable.userId eq userId) }.empty()
                .not()

        when {
            moveToId == null -> if (costsFromDeletedCategoryExists) throw BadRequestException("A categoria selecionada possui custos, mova-os")
            else -> {
                val moveToCategoryExists = CategoriesTable.selectAll()
                    .where((CategoriesTable.id eq moveToId) and ((CategoriesTable.userId eq userId) or (CategoriesTable.userId.isNull())))
                    .empty().not()

                if (!moveToCategoryExists) {
                    throw BadRequestException("moveTo inválido")
                }
                CostsTable.update(where = { CostsTable.categoryId eq id }) { it[categoryId] = moveToId }

            }
        }

        CategoriesTable.deleteWhere {
            (CategoriesTable.id eq id) and (CategoriesTable.userId eq userId)
        } > 0


    }
}
