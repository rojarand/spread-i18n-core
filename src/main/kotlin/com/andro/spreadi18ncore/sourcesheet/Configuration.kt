package com.andro.spreadi18ncore.sourcesheet

import com.andro.spreadi18ncore.importing.ImportException
import com.andro.spreadi18ncore.sourcetargetmatching.Locales.Companion.allLocales
import com.andro.spreadi18ncore.sourcetargetmatching.MatchedSourcesAndTargets
import com.andro.spreadi18ncore.sourcetargetmatching.SourceColumn
import com.andro.spreadi18ncore.targetproject.ProjectType
import org.apache.poi.ss.usermodel.Cell
import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.ss.usermodel.Sheet

internal enum class TranslationKeyType {
    iOS {
        override val cellText: List<String>
            get() = listOf("ios")
    },
    Android {
        override val cellText: List<String>
            get() = listOf("android")
    },
    General {
        override val cellText: List<String>
            get() = listOf("key", "identifier", "id")
    };
    abstract val cellText: List<String>
}

internal class ColumnNotFound(): ImportException("")

internal data class TranslationKeyColumn(val columnIndex: Int, val translationKeyType: TranslationKeyType)

internal class TranslationKeyColumns() {
    private val translationKeyColumns = mutableSetOf<TranslationKeyColumn>()
    fun add(translationKeyColumn: TranslationKeyColumn) {
        translationKeyColumns.add(translationKeyColumn)
    }
    fun isNotEmpty() = translationKeyColumns.isNotEmpty()

    private fun findTranslationKeyColumn(translationKeyType: TranslationKeyType): TranslationKeyColumn? {
        return translationKeyColumns.find { translationKeyType == it.translationKeyType }
    }

    fun containsTranslationKeyColumnFor(translationKeyType: TranslationKeyType): Boolean {
        return findTranslationKeyColumn(translationKeyType)!=null
    }

    fun getTranslationKeyColumn(translationKeyType: TranslationKeyType): TranslationKeyColumn {
        return findTranslationKeyColumn(translationKeyType) ?: throw ColumnNotFound()
    }
}

internal class ConfigRowNotFound(): ImportException("Config row not found in the source file.")

internal data class ConfigRow(val rowInDocument: Int, val sourceColumns: Set<SourceColumn>,
                              private val translationKeyColumns: TranslationKeyColumns
) {

    fun indexOfTranslationKeyColumnForProjectType(projectType: ProjectType): Int {
        if (translationKeyColumns.containsTranslationKeyColumnFor(projectType.translationKeyType)) {
            return translationKeyColumns.getTranslationKeyColumn(projectType.translationKeyType).columnIndex
        }
        return translationKeyColumns.getTranslationKeyColumn(TranslationKeyType.General).columnIndex
    }

    val rowWithFirstTranslation = rowInDocument+1

    companion object {

        fun getFrom(sheet: Sheet): ConfigRow {
            return findIn(sheet)
                    ?: throw ConfigRowNotFound()
        }

        fun findIn(sheet: Sheet): ConfigRow? {
            sheet.rowIterator().withIndex().forEach { indexedRow ->
                val configRowIdentifier =
                    ConfigRowIdentifier()
                indexedRow.value.cellIterator().withIndex().forEach { indexedCell ->
                    configRowIdentifier.analyseCell(indexedCell)
                }
                if (configRowIdentifier.isConfigRow) {
                    return ConfigRow(
                        indexedRow.index,
                        configRowIdentifier.localeColumns,
                        configRowIdentifier.translationKeyColumns
                    )
                }
            }
            return null
        }
    }
}

internal class ConfigRowIdentifier {
    val translationKeyColumns = TranslationKeyColumns()
    val localeColumns = mutableSetOf<SourceColumn>()
    val unknownPurposeColumns = mutableListOf<IndexedValue<Cell>>()

    fun analyseCell(indexedCell: IndexedValue<Cell>) {
        if (indexedCell.value.cellType == CellType.STRING) {
            listOf(::storeIfLocale, ::storeIfTranslationKey, ::storeUnknownPurposeColumn).forEach { store ->
                if( store(indexedCell) ) {
                    return
                }
            }
        }
    }

    private fun storeIfLocale(localeCellCandidate: IndexedValue<Cell>): Boolean {
        val localeCandidate = localeCellCandidate.value.stringCellValue.trim()
        if (localeCandidate.isEmpty()) {
            return false
        }
        val locale = allLocales.findLocale(localeCandidate)
        if (locale != null) {
            localeColumns.add(
                SourceColumn(
                    localeCandidate,
                    localeCellCandidate.index
                )
            )
            return true
        }
        return false
    }

    private fun storeIfTranslationKey(translationKeyCellCandidate: IndexedValue<Cell>): Boolean {
        val tokens = translationKeyCellCandidate.value.stringCellValue.trim()
                .split(" ").filter { it.isNotBlank() }.map { it.toLowerCase() }
        if (tokens.isEmpty()) {
            return false
        }
        for (translationKeyType in TranslationKeyType.values()) {
            val id = translationKeyType.cellText.find { tokens.contains(it) }
            if (id != null) {
                translationKeyColumns.add(
                    TranslationKeyColumn(
                        translationKeyCellCandidate.index,
                        translationKeyType
                    )
                )
                return true
            }
        }
        return false
    }

    private fun storeUnknownPurposeColumn(indexedCell: IndexedValue<Cell>): Boolean {
        unknownPurposeColumns.add(indexedCell)
        return true
    }

    val isConfigRow: Boolean
        get() {
            return translationKeyColumns.isNotEmpty() && localeColumns.isNotEmpty()
        }
}