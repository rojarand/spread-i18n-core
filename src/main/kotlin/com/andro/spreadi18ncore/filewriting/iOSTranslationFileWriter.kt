package com.andro.spreadi18ncore.filewriting

import java.io.BufferedWriter
import java.io.IOException
import java.lang.StringBuilder
import java.nio.file.Files
import java.nio.file.Path

@Suppress("ClassName")
internal class iOSHeaderCopying {
    companion object {

        fun copyHeaderFromFileWith(path: Path): String? {
            return findEndPositionOfHeaderInFileWith(
                path
            )?.let { endPositionOfHeader ->
                copyHeaderFromFileWith(
                    path,
                    endPositionOfHeader
                )
            }
        }

        private fun findEndPositionOfHeaderInFileWith(path: Path): Int? {
            Files.newBufferedReader(path).use { reader ->
                return reader.lineSequence().withIndex().find { it.value.contains("*/") }?.index
            }
        }

        private fun copyHeaderFromFileWith(path: Path, endPositionOfHeader: Int): String {
            val sb = StringBuilder()
            Files.newBufferedReader(path).use { reader ->
                reader.lineSequence().withIndex().take(endPositionOfHeader + 1).forEach { line ->
                    sb.append(line.value + "\n")
                }
            }
            return sb.toString()
        }
    }
}

@Suppress("ClassName")
internal class iOSTranslationFileWriter(private val targetDirectoryPath: Path) :
    TranslationFileWriter {

    private val localizableFilePath: Path by lazy {
        targetDirectoryPath.resolve("Localizable.strings")
    }
    private val localizableWriter: BufferedWriter by lazy {
        Files.newBufferedWriter(localizableFilePath)
    }
    private val infoPlistFilePath: Path by lazy {
        targetDirectoryPath.resolve("InfoPlist.strings")
    }
    private val infoPlistWriter: BufferedWriter by lazy {
        Files.newBufferedWriter(infoPlistFilePath)
    }

    init {
        //https://stackoverflow.com/questions/25736700/how-to-localise-a-string-inside-the-ios-info-plist-file
        try {
            if (Files.exists(localizableFilePath)) {
                iOSHeaderCopying.copyHeaderFromFileWith(
                    localizableFilePath
                )?.let { header ->
                    localizableWriter.write(header)
                }
            } else {
                Files.createFile(localizableFilePath)
            }
            if (Files.exists(infoPlistFilePath)) {
                iOSHeaderCopying.copyHeaderFromFileWith(
                    infoPlistFilePath
                )?.let { header ->
                    infoPlistWriter.write(header)
                }
            } else {
                Files.createFile(infoPlistFilePath)
            }
        } catch (exc: IOException) {
            throw CanNotAccessLocalizationFile(exc)
        }
    }

    override fun write(key: String, value: String) {
        val isSystemTranslation = listOf("NS", "CF").firstOrNull { key.startsWith(it) } != null
        if (isSystemTranslation) {
            infoPlistWriter.write("\"$key\" = \"$value\";\n")
        } else {
            if (key.startsWith("//")) {
                localizableWriter.write("$key\n")
            } else if (key.isNotBlank()) {
                localizableWriter.write("\"$key\" = \"$value\";\n")
            }
        }
    }

    override fun close() {
        localizableWriter.flush()
        localizableWriter.close()

        infoPlistWriter.flush()
        infoPlistWriter.close()
    }
}

