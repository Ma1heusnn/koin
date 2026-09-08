package com.koin.models

import kotlinx.serialization.Serializable

@Serializable
data class Category(
    val id: Int? = null,
    val name: String,
    val icon: String = "none",
    val color: String = "#CCCCCC",
    val userId: Int? = null
)
@Serializable
data class CategoryDTO(
    val name: String,
    val icon: String = "none",
    val color: String = "#CCCCCC"
)

@Serializable
data class CategoryPatch(
    val name: String? = null,
    val icon: String? = null,
    val color: String? = null
)

private val HEX_COLOR = Regex("^#[0-9A-Fa-f]{6}$")

private val ICONS = setOf(
    "health", "food", "leisure", "transport", "education", "investments", "none"
)

fun CategoryDTO.validate(): List<String> = buildList {
    if (name.isBlank()) add("O nome da categoria é obrigatório")
    if (name.length > 100) add("O nome da categoria possui mais caracteres que o limite")
    if (icon !in ICONS) add ("Ícone inválido. Válidos: ${ICONS.joinToString()}")
    if (!color.matches(HEX_COLOR)) add("Formato de cor inválido (#RRGGBB esperado)")
}

fun CategoryPatch.validate(): List<String> = buildList {
    if (listOfNotNull(name, icon, color).isEmpty()) add("Envie ao menos um campo para atualizar")
    name?.let { if (name.isBlank()) add("O nome da categoria é obrigatório")}
    name?.let { if (name.length > 100) add("O nome da categoria possui mais caracteres que o limite") }
    icon?.let { if (it !in ICONS) add("Ícone inválido. Válidos: ${ICONS.joinToString()}") }
    color?.let { if (!color.matches(HEX_COLOR)) add("Formato de cor inválido (#RRGGBB esperado)")}
}