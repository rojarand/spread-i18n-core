package com.andro.spreadi18ncore

import com.andro.spreadi18ncore.sourcesheet.TranslationKeyType
import com.andro.spreadi18ncore.filewriting.TranslationFileWriter
import com.andro.spreadi18ncore.importing.Importer
import com.andro.spreadi18ncore.sourcetargetmatching.AndroidSourceTargetMatcher
import com.andro.spreadi18ncore.targetproject.ProjectType
import com.andro.spreadi18ncore.targetproject.TargetProject
import com.andro.spreadi18ncore.valuetransformation.AndroidDefaultValueTransformation
import io.mockk.*
import org.junit.jupiter.api.Test
import java.nio.file.Path

class ImportTest {

    @Test
    fun import_of_android_translations() {
        //arrange
        val sheetContent = """
            ┌─────────────────────────────────────┐
            │Key              │English │Polish    │
            ├─────────────────────────────────────┤
            │btn_cancel_text  │Cancel  │Anuluj    │
            ├─────────────────────────────────────┤
            │btn_apply_text   │Apply   │Zastosuj  │
            └─────────────────────────────────────┘
        """
        val sheet = mockSheet(sheetContent)
        val project = mockk<TargetProject>()
        val projectType = mockk<ProjectType>()
        every { projectType.sourceTargetMatcher } returns AndroidSourceTargetMatcher()
        every { projectType.translationKeyType } returns TranslationKeyType.Android
        every { projectType.valueTransformation } returns AndroidDefaultValueTransformation()

        val enFileWriter = spyk(mockk<TranslationFileWriter>(relaxed = true))
        val plFileWriter = spyk(mockk<TranslationFileWriter>(relaxed = true))

        every { projectType.fileWriter(Path.of("values")) } returns enFileWriter
        every { projectType.fileWriter(Path.of("values-pl")) } returns plFileWriter
        every { project.type } returns projectType
        every { project.localizationDirectories } returns listOf("values".asTargetDir(), "values-pl".asTargetDir())

        //act
        val importer =
            Importer(sourceSheet = sheet, targetProject = project)
        importer.import()

        //assert
        verify { enFileWriter.write("btn_cancel_text", "Cancel") }
        verify { enFileWriter.write("btn_apply_text", "Apply") }

        verify { plFileWriter.write("btn_cancel_text", "Anuluj") }
        verify { plFileWriter.write("btn_apply_text", "Zastosuj") }
    }

}